package dev.denismasterherobrine.finale.arenaobservability.incident;

import dev.denismasterherobrine.finale.arenaobservability.health.ReasonCode;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record Incident(
        String id,
        IncidentCode code,
        Severity severity,
        String arenaId,
        Set<ReasonCode> reasons,
        Map<String, Double> keyMetrics,
        long timestampMs,
        boolean resolved
) {
    public static Incident create(IncidentCode code, Severity severity, String arenaId,
                                  Set<ReasonCode> reasons, Map<String, Double> keyMetrics) {
        return new Incident(
                UUID.randomUUID().toString().substring(0, 8),
                code, severity, arenaId, reasons, keyMetrics,
                System.currentTimeMillis(), false
        );
    }

    public Incident resolve() {
        return new Incident(id, code, severity, arenaId, reasons, keyMetrics, timestampMs, true);
    }

    public enum Severity {
        WARNING, CRITICAL
    }
}
