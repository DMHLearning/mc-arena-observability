package dev.denismasterherobrine.finale.arenaobservability.detector;

import dev.denismasterherobrine.finale.arenaobservability.metric.store.MetricStore;

/**
 * Anomalous death detection is event-driven (handled in PlayerDeathListener).
 * This detector exists as a periodic check to track the rate of anomalous deaths
 * and can be extended to raise incidents if the rate exceeds a threshold.
 */
public class AnomalousDeathDetector implements FailureDetector {

    private final MetricStore store;
    private long previousCount = 0;
    private long previousCheckMs = System.currentTimeMillis();

    public AnomalousDeathDetector(MetricStore store) {
        this.store = store;
    }

    @Override
    public void tick() {
        long currentCount = store.getCounter("anomalous_death");
        long now = System.currentTimeMillis();
        long elapsedMs = now - previousCheckMs;

        if (elapsedMs > 0) {
            long delta = currentCount - previousCount;
            double ratePerMinute = (delta * 60_000.0) / elapsedMs;
            store.setGauge("anomalous_death_rate_per_min", (long) ratePerMinute);
        }

        previousCount = currentCount;
        previousCheckMs = now;
    }
}
