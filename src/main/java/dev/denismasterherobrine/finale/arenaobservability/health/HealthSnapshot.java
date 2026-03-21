package dev.denismasterherobrine.finale.arenaobservability.health;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public record HealthSnapshot(
        HealthState state,
        Set<ReasonCode> reasons,
        Map<String, Double> keyMetrics,
        long timestampMs
) {
    public static HealthSnapshot healthy() {
        return new HealthSnapshot(HealthState.HEALTHY, Collections.emptySet(), Collections.emptyMap(), System.currentTimeMillis());
    }
}
