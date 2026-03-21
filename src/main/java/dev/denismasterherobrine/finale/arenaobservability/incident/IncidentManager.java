package dev.denismasterherobrine.finale.arenaobservability.incident;

import dev.denismasterherobrine.finale.arenaobservability.config.ObservabilityConfig;
import dev.denismasterherobrine.finale.arenaobservability.health.ArenaHealthEvaluator;
import dev.denismasterherobrine.finale.arenaobservability.health.ReasonCode;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public class IncidentManager {

    private final ObservabilityConfig config;
    private final ArenaHealthEvaluator arenaHealthEvaluator;
    private final Logger logger;

    private final Map<String, Incident> activeIncidents = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> incidentCounters = new ConcurrentHashMap<>();
    private final Map<String, Long> rateLimitMap = new ConcurrentHashMap<>();

    public IncidentManager(ObservabilityConfig config, ArenaHealthEvaluator arenaHealthEvaluator, Logger logger) {
        this.config = config;
        this.arenaHealthEvaluator = arenaHealthEvaluator;
        this.logger = logger;
    }

    public Incident raise(IncidentCode code, Incident.Severity severity, String arenaId,
                          Set<ReasonCode> reasons, Map<String, Double> keyMetrics) {
        String key = code.name() + ":" + (arenaId != null ? arenaId : "server");

        if (isRateLimited(key)) {
            return null;
        }

        Incident incident = Incident.create(code, severity, arenaId, reasons, keyMetrics);
        activeIncidents.put(key, incident);
        incidentCounters.computeIfAbsent(code.name(), k -> new AtomicLong()).incrementAndGet();

        arenaHealthEvaluator.onIncidentRaised(incident);

        logger.info(String.format("[Incident] RAISED %s severity=%s arena=%s reasons=%s metrics=%s",
                code, severity, arenaId, reasons, keyMetrics));

        return incident;
    }

    public void resolve(IncidentCode code, String arenaId) {
        String key = code.name() + ":" + (arenaId != null ? arenaId : "server");
        Incident incident = activeIncidents.remove(key);
        if (incident != null) {
            if (arenaId != null) {
                arenaHealthEvaluator.onIncidentResolved(arenaId, code);
            }
            logger.info(String.format("[Incident] RESOLVED %s arena=%s", code, arenaId));
        }
    }

    public boolean hasActive(IncidentCode code, String arenaId) {
        String key = code.name() + ":" + (arenaId != null ? arenaId : "server");
        return activeIncidents.containsKey(key);
    }

    public Collection<Incident> getActiveIncidents() {
        return Collections.unmodifiableCollection(activeIncidents.values());
    }

    public long getIncidentCount(IncidentCode code) {
        AtomicLong counter = incidentCounters.get(code.name());
        return counter != null ? counter.get() : 0;
    }

    public long getTotalIncidentCount() {
        return incidentCounters.values().stream().mapToLong(AtomicLong::get).sum();
    }

    private boolean isRateLimited(String key) {
        long now = System.currentTimeMillis();
        Long lastTime = rateLimitMap.get(key);
        long windowMs = 60_000L / Math.max(1, config.getIncidentPerArenaPerMinute());

        if (lastTime != null && now - lastTime < windowMs) {
            return true;
        }
        rateLimitMap.put(key, now);
        return false;
    }
}
