package dev.denismasterherobrine.finale.arenaobservability.metric.collector;

import dev.cubxity.plugins.metrics.api.metric.collector.Collector;
import dev.cubxity.plugins.metrics.api.metric.data.GaugeMetric;
import dev.cubxity.plugins.metrics.api.metric.data.Metric;
import dev.denismasterherobrine.finale.arenaobservability.metric.store.MetricStore;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class WaveMetricCollector implements Collector {

    private final MetricStore store;

    public WaveMetricCollector(MetricStore store) {
        this.store = store;
    }

    @Override
    public @NotNull List<Metric> collect() {
        List<Metric> metrics = new ArrayList<>();

        for (Map.Entry<String, AtomicLong> entry : store.getAllGauges().entrySet()) {
            if (entry.getKey().startsWith("wave_index:")) {
                String arenaId = entry.getKey().substring("wave_index:".length());
                metrics.add(new GaugeMetric("arena_wave_index", Map.of("arena_id", arenaId), entry.getValue().get()));
            }
        }

        return metrics;
    }
}
