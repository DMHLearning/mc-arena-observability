package dev.denismasterherobrine.finale.arenaobservability.metric.store;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;

/**
 * Thread-safe store for counters (monotonic totals) and gauges (point-in-time values).
 * Designed to be read by UnifiedMetrics collectors on an async scrape thread
 * and written to by Bukkit listeners on the main/scheduler threads.
 */
public class MetricStore {

    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();
    private final Map<String, DoubleAdder> doubleCounters = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> gauges = new ConcurrentHashMap<>();

    // --- Wave duration tracking (simple ring buffer per arena) ---
    private final Map<String, WaveDurationBuffer> waveDurations = new ConcurrentHashMap<>();

    // --- Last mob death timestamps per arena (for wave-stuck detection) ---
    private final Map<String, AtomicLong> lastMobDeathMs = new ConcurrentHashMap<>();

    public void incrementCounter(String key) {
        counters.computeIfAbsent(key, k -> new AtomicLong()).incrementAndGet();
    }

    public void addCounter(String key, long delta) {
        counters.computeIfAbsent(key, k -> new AtomicLong()).addAndGet(delta);
    }

    public long getCounter(String key) {
        AtomicLong v = counters.get(key);
        return v != null ? v.get() : 0;
    }

    public void setGauge(String key, long value) {
        gauges.computeIfAbsent(key, k -> new AtomicLong()).set(value);
    }

    public long getGauge(String key) {
        AtomicLong v = gauges.get(key);
        return v != null ? v.get() : 0;
    }

    public void recordWaveDuration(String arenaId, long durationMs) {
        waveDurations.computeIfAbsent(arenaId, k -> new WaveDurationBuffer(50)).add(durationMs);
    }

    public double getWaveDurationP95(String arenaId) {
        WaveDurationBuffer buf = waveDurations.get(arenaId);
        return buf != null ? buf.percentile(0.95) : 0;
    }

    public void recordMobDeath(String arenaId) {
        lastMobDeathMs.computeIfAbsent(arenaId, k -> new AtomicLong()).set(System.currentTimeMillis());
    }

    public long getLastMobDeathMs(String arenaId) {
        AtomicLong v = lastMobDeathMs.get(arenaId);
        return v != null ? v.get() : 0;
    }

    public void removeArena(String arenaId) {
        waveDurations.remove(arenaId);
        lastMobDeathMs.remove(arenaId);
    }

    public Map<String, AtomicLong> getAllCounters() {
        return Collections.unmodifiableMap(counters);
    }

    public Map<String, AtomicLong> getAllGauges() {
        return Collections.unmodifiableMap(gauges);
    }

    /**
     * Simple ring buffer for computing percentiles of wave durations.
     */
    static class WaveDurationBuffer {
        private final long[] values;
        private int pos = 0;
        private int size = 0;

        WaveDurationBuffer(int capacity) {
            this.values = new long[capacity];
        }

        synchronized void add(long value) {
            values[pos] = value;
            pos = (pos + 1) % values.length;
            if (size < values.length) size++;
        }

        synchronized double percentile(double pct) {
            if (size == 0) return 0;
            long[] sorted = new long[size];
            System.arraycopy(values, 0, sorted, 0, size);
            java.util.Arrays.sort(sorted);
            int idx = (int) Math.ceil(pct * size) - 1;
            return sorted[Math.max(0, idx)];
        }
    }
}
