package dev.denismasterherobrine.finale.arenaobservability.metric.collector;

import dev.cubxity.plugins.metrics.api.metric.collector.Collector;
import dev.cubxity.plugins.metrics.api.metric.data.CounterMetric;
import dev.cubxity.plugins.metrics.api.metric.data.GaugeMetric;
import dev.cubxity.plugins.metrics.api.metric.data.Metric;
import dev.denismasterherobrine.finale.arenaobservability.metric.store.MetricStore;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class ArenaSessionMetricCollector implements Collector {

    private final MetricStore store;

    public ArenaSessionMetricCollector(MetricStore store) {
        this.store = store;
    }

    @Override
    public @NotNull List<Metric> collect() {
        List<Metric> metrics = new ArrayList<>();

        metrics.add(new GaugeMetric("arena_sessions_active", Map.of(), store.getGauge("sessions_active")));
        metrics.add(new GaugeMetric("arena_players_in_arenas", Map.of(), store.getGauge("players_in_arenas")));

        for (Map.Entry<String, AtomicLong> entry : store.getAllCounters().entrySet()) {
            if (entry.getKey().startsWith("session_start:")) {
                String arenaId = entry.getKey().substring("session_start:".length());
                metrics.add(new CounterMetric("arena_session_start_total", Map.of("arena_id", arenaId), entry.getValue().get()));
            } else if (entry.getKey().startsWith("session_end:")) {
                String[] parts = entry.getKey().split(":");
                if (parts.length >= 3) {
                    metrics.add(new CounterMetric("arena_session_end_total",
                            Map.of("arena_id", parts[1], "reason", parts[2]), entry.getValue().get()));
                }
            } else if (entry.getKey().startsWith("soft_fail:")) {
                String arenaId = entry.getKey().substring("soft_fail:".length());
                metrics.add(new CounterMetric("arena_soft_fail_total", Map.of("arena_id", arenaId), entry.getValue().get()));
            }
        }

        return metrics;
    }
}
