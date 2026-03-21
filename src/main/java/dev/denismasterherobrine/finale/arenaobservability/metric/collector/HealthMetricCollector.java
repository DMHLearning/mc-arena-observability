package dev.denismasterherobrine.finale.arenaobservability.metric.collector;

import dev.cubxity.plugins.metrics.api.metric.collector.Collector;
import dev.cubxity.plugins.metrics.api.metric.data.CounterMetric;
import dev.cubxity.plugins.metrics.api.metric.data.GaugeMetric;
import dev.cubxity.plugins.metrics.api.metric.data.Metric;
import dev.denismasterherobrine.finale.arenaobservability.health.HealthState;
import dev.denismasterherobrine.finale.arenaobservability.health.ServerHealthEvaluator;
import dev.denismasterherobrine.finale.arenaobservability.incident.IncidentCode;
import dev.denismasterherobrine.finale.arenaobservability.incident.IncidentManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HealthMetricCollector implements Collector {

    private final ServerHealthEvaluator healthEvaluator;
    private final IncidentManager incidentManager;

    public HealthMetricCollector(ServerHealthEvaluator healthEvaluator, IncidentManager incidentManager) {
        this.healthEvaluator = healthEvaluator;
        this.incidentManager = incidentManager;
    }

    @Override
    public @NotNull List<Metric> collect() {
        List<Metric> metrics = new ArrayList<>();

        HealthState state = healthEvaluator.getCurrentState();
        for (HealthState hs : HealthState.values()) {
            metrics.add(new GaugeMetric("arena_homeostasis_state",
                    Map.of("state", hs.name()), hs == state ? 1 : 0));
        }

        for (IncidentCode code : IncidentCode.values()) {
            metrics.add(new CounterMetric("arena_incident_raised_total",
                    Map.of("code", code.name()), incidentManager.getIncidentCount(code)));
        }

        return metrics;
    }
}
