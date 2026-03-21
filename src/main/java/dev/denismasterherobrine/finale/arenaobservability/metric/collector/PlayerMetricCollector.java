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

public class PlayerMetricCollector implements Collector {

    private final MetricStore store;

    public PlayerMetricCollector(MetricStore store) {
        this.store = store;
    }

    @Override
    public @NotNull List<Metric> collect() {
        List<Metric> metrics = new ArrayList<>();

        for (Map.Entry<String, AtomicLong> entry : store.getAllCounters().entrySet()) {
            if (entry.getKey().startsWith("death:")) {
                String cause = entry.getKey().substring("death:".length());
                metrics.add(new CounterMetric("player_death_total", Map.of("cause", cause), entry.getValue().get()));
            }
        }

        metrics.add(new CounterMetric("anomalous_death_suspected_total", Map.of(), store.getCounter("anomalous_death")));

        return metrics;
    }
}
