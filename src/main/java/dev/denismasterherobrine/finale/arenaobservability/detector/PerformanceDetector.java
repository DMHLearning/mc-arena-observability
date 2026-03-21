package dev.denismasterherobrine.finale.arenaobservability.detector;

import dev.denismasterherobrine.finale.arenaobservability.health.HealthState;
import dev.denismasterherobrine.finale.arenaobservability.health.ReasonCode;
import dev.denismasterherobrine.finale.arenaobservability.health.ServerHealthEvaluator;
import dev.denismasterherobrine.finale.arenaobservability.incident.Incident;
import dev.denismasterherobrine.finale.arenaobservability.incident.IncidentCode;
import dev.denismasterherobrine.finale.arenaobservability.incident.IncidentManager;
import org.bukkit.Bukkit;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public class PerformanceDetector implements FailureDetector {

    private final ServerHealthEvaluator healthEvaluator;
    private final IncidentManager incidentManager;
    private HealthState previousState = HealthState.HEALTHY;

    public PerformanceDetector(ServerHealthEvaluator healthEvaluator, IncidentManager incidentManager) {
        this.healthEvaluator = healthEvaluator;
        this.incidentManager = incidentManager;
    }

    @Override
    public void tick() {
        HealthState current = healthEvaluator.getCurrentState();
        double tps = Bukkit.getTPS()[0];
        double mspt = Bukkit.getAverageTickTime();

        if (current == HealthState.DEGRADED && previousState == HealthState.HEALTHY) {
            Set<ReasonCode> reasons = EnumSet.noneOf(ReasonCode.class);
            reasons.add(ReasonCode.PERF_HIGH_MSPT);
            reasons.add(ReasonCode.PERF_LOW_TPS);
            incidentManager.raise(IncidentCode.PERF_DEGRADED, Incident.Severity.WARNING, null,
                    reasons, Map.of("tps", tps, "mspt", mspt));
        }

        if (current == HealthState.CRITICAL && previousState.ordinal() < HealthState.CRITICAL.ordinal()) {
            Set<ReasonCode> reasons = EnumSet.noneOf(ReasonCode.class);
            reasons.add(ReasonCode.PERF_HIGH_MSPT);
            reasons.add(ReasonCode.PERF_LOW_TPS);
            incidentManager.raise(IncidentCode.PERF_CRITICAL, Incident.Severity.CRITICAL, null,
                    reasons, Map.of("tps", tps, "mspt", mspt));
        }

        if (current == HealthState.HEALTHY && previousState != HealthState.HEALTHY) {
            incidentManager.resolve(IncidentCode.PERF_DEGRADED, null);
            incidentManager.resolve(IncidentCode.PERF_CRITICAL, null);
        }

        previousState = current;
    }
}
