package dev.denismasterherobrine.finale.arenaobservability.metric.collector;

import dev.cubxity.plugins.metrics.api.metric.collector.Collector;
import dev.cubxity.plugins.metrics.api.metric.data.CounterMetric;
import dev.cubxity.plugins.metrics.api.metric.data.Metric;
import dev.denismasterherobrine.finale.arenaobservability.metric.store.MetricStore;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class ResetMetricCollector implements Collector {

    private final MetricStore store;

    public ResetMetricCollector(MetricStore store) {
        this.store = store;
    }

    @Override
    public @NotNull List<Metric> collect() {
        List<Metric> metrics = new ArrayList<>();

        for (Map.Entry<String, AtomicLong> entry : store.getAllCounters().entrySet()) {
            if (entry.getKey().startsWith("reset_success:")) {
                String arenaId = entry.getKey().substring("reset_success:".length());
                metrics.add(new CounterMetric("arena_reset_success_total", Map.of("arena_id", arenaId), entry.getValue().get()));
            } else if (entry.getKey().startsWith("reset_fail:")) {
                String arenaId = entry.getKey().substring("reset_fail:".length());
                metrics.add(new CounterMetric("arena_reset_fail_total", Map.of("arena_id", arenaId), entry.getValue().get()));
            }
        }

        return metrics;
    }
}
