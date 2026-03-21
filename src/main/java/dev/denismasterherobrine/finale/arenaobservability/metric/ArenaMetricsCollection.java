package dev.denismasterherobrine.finale.arenaobservability.metric;

import dev.cubxity.plugins.metrics.api.metric.collector.Collector;
import dev.cubxity.plugins.metrics.api.metric.collector.CollectorCollection;
import dev.denismasterherobrine.finale.arenaobservability.health.ServerHealthEvaluator;
import dev.denismasterherobrine.finale.arenaobservability.incident.IncidentManager;
import dev.denismasterherobrine.finale.arenaobservability.metric.collector.*;
import dev.denismasterherobrine.finale.arenaobservability.metric.store.MetricStore;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ArenaMetricsCollection implements CollectorCollection {

    private final List<Collector> collectors;

    public ArenaMetricsCollection(MetricStore store, ServerHealthEvaluator healthEvaluator, IncidentManager incidentManager) {
        this.collectors = List.of(
                new ArenaSessionMetricCollector(store),
                new WaveMetricCollector(store),
                new ResetMetricCollector(store),
                new PlayerMetricCollector(store),
                new ErrorMetricCollector(store),
                new HealthMetricCollector(healthEvaluator, incidentManager)
        );
    }

    @Override
    public @NotNull List<Collector> getCollectors() {
        return collectors;
    }

    @Override
    public boolean isAsync() {
        return true;
    }

    @Override
    public void initialize() {
        // no-op
    }

    @Override
    public void dispose() {
        // no-op
    }
}
