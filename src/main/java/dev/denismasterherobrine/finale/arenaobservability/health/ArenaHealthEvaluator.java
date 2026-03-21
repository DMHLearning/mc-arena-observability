package dev.denismasterherobrine.finale.arenaobservability.health;

import dev.denismasterherobrine.finale.arenaobservability.incident.Incident;
import dev.denismasterherobrine.finale.arenaobservability.incident.IncidentCode;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ArenaHealthEvaluator {

    private final Map<String, HealthState> arenaStates = new ConcurrentHashMap<>();

    public void onIncidentRaised(Incident incident) {
        if (incident.arenaId() == null) return;

        HealthState escalated = switch (incident.code()) {
            case WAVE_STUCK -> HealthState.DEGRADED;
            case RESET_FAILED -> HealthState.CRITICAL;
            case PERF_DEGRADED -> HealthState.DEGRADED;
            case PERF_CRITICAL -> HealthState.CRITICAL;
            default -> HealthState.DEGRADED;
        };

        arenaStates.merge(incident.arenaId(), escalated, HealthState::worse);
    }

    public void onIncidentResolved(String arenaId, IncidentCode code) {
        arenaStates.remove(arenaId);
    }

    public void removeArena(String arenaId) {
        arenaStates.remove(arenaId);
    }

    public HealthState getArenaHealth(String arenaId) {
        return arenaStates.getOrDefault(arenaId, HealthState.HEALTHY);
    }

    public Map<String, HealthState> getAllArenaStates() {
        return Collections.unmodifiableMap(arenaStates);
    }
}
