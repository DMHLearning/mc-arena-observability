package dev.denismasterherobrine.finale.arenaobservability.health;

import dev.denismasterherobrine.finale.arenaobservability.config.ObservabilityConfig;
import org.bukkit.Bukkit;

import java.util.*;

public class ServerHealthEvaluator {

    private final ObservabilityConfig config;

    private HealthState currentState = HealthState.HEALTHY;
    private long degradedSinceMs = 0;
    private long healthySinceMs = System.currentTimeMillis();
    private long criticalSinceMs = 0;
    private long criticalRecoverSinceMs = 0;

    public ServerHealthEvaluator(ObservabilityConfig config) {
        this.config = config;
    }

    public HealthSnapshot evaluate() {
        double tps = Bukkit.getTPS()[0];
        double mspt = Bukkit.getAverageTickTime();
        long now = System.currentTimeMillis();

        Set<ReasonCode> reasons = EnumSet.noneOf(ReasonCode.class);
        HealthState desired = HealthState.HEALTHY;

        if (tps < config.getTpsCriticalBelow()) {
            desired = HealthState.CRITICAL;
            reasons.add(ReasonCode.PERF_LOW_TPS);
        } else if (tps < config.getTpsDegradedBelow()) {
            desired = desired.worse(HealthState.DEGRADED);
            reasons.add(ReasonCode.PERF_LOW_TPS);
        }

        if (mspt > config.getMsptCriticalAbove()) {
            desired = desired.worse(HealthState.CRITICAL);
            reasons.add(ReasonCode.PERF_HIGH_MSPT);
        } else if (mspt > config.getMsptDegradedAbove()) {
            desired = desired.worse(HealthState.DEGRADED);
            reasons.add(ReasonCode.PERF_HIGH_MSPT);
        }

        currentState = applyHysteresis(desired, now);

        Map<String, Double> metrics = new LinkedHashMap<>();
        metrics.put("tps", tps);
        metrics.put("mspt", mspt);

        return new HealthSnapshot(currentState, reasons, metrics, now);
    }

    private HealthState applyHysteresis(HealthState desired, long now) {
        switch (currentState) {
            case HEALTHY -> {
                if (desired == HealthState.CRITICAL) {
                    if (criticalSinceMs == 0) criticalSinceMs = now;
                    if (now - criticalSinceMs >= config.getCriticalEnterSeconds() * 1000L) {
                        resetTimers();
                        return HealthState.CRITICAL;
                    }
                    return HealthState.HEALTHY;
                }
                if (desired == HealthState.DEGRADED) {
                    if (degradedSinceMs == 0) degradedSinceMs = now;
                    if (now - degradedSinceMs >= config.getDegradeEnterSeconds() * 1000L) {
                        resetTimers();
                        return HealthState.DEGRADED;
                    }
                    return HealthState.HEALTHY;
                }
                resetTimers();
                return HealthState.HEALTHY;
            }
            case DEGRADED -> {
                if (desired == HealthState.CRITICAL) {
                    if (criticalSinceMs == 0) criticalSinceMs = now;
                    if (now - criticalSinceMs >= config.getCriticalEnterSeconds() * 1000L) {
                        resetTimers();
                        return HealthState.CRITICAL;
                    }
                    return HealthState.DEGRADED;
                }
                if (desired == HealthState.HEALTHY) {
                    if (healthySinceMs == 0) healthySinceMs = now;
                    if (now - healthySinceMs >= config.getDegradeExitSeconds() * 1000L) {
                        resetTimers();
                        return HealthState.HEALTHY;
                    }
                    return HealthState.DEGRADED;
                }
                healthySinceMs = 0;
                criticalSinceMs = 0;
                return HealthState.DEGRADED;
            }
            case CRITICAL -> {
                if (desired.ordinal() < HealthState.CRITICAL.ordinal()) {
                    if (criticalRecoverSinceMs == 0) criticalRecoverSinceMs = now;
                    if (now - criticalRecoverSinceMs >= config.getCriticalExitSeconds() * 1000L) {
                        resetTimers();
                        return desired == HealthState.HEALTHY ? HealthState.DEGRADED : desired;
                    }
                    return HealthState.CRITICAL;
                }
                criticalRecoverSinceMs = 0;
                return HealthState.CRITICAL;
            }
            default -> { return currentState; }
        }
    }

    private void resetTimers() {
        degradedSinceMs = 0;
        healthySinceMs = 0;
        criticalSinceMs = 0;
        criticalRecoverSinceMs = 0;
    }

    public HealthState getCurrentState() {
        return currentState;
    }
}
