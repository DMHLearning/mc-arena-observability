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

public class ErrorMetricCollector implements Collector {

    private final MetricStore store;

    public ErrorMetricCollector(MetricStore store) {
        this.store = store;
    }

    @Override
    public @NotNull List<Metric> collect() {
        List<Metric> metrics = new ArrayList<>();

        for (Map.Entry<String, AtomicLong> entry : store.getAllCounters().entrySet()) {
            if (entry.getKey().startsWith("exception:")) {
                String module = entry.getKey().substring("exception:".length());
                metrics.add(new CounterMetric("arena_exceptions_total", Map.of("module", module), entry.getValue().get()));
            }
        }

        metrics.add(new GaugeMetric("arena_error_log_rate", Map.of(), store.getGauge("error_log_rate")));

        return metrics;
    }
}
