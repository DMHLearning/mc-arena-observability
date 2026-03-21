package dev.denismasterherobrine.finale.arenaobservability.scheduler;

import dev.denismasterherobrine.finale.arenaobservability.bridge.MatchmakerBridge;
import dev.denismasterherobrine.finale.arenaobservability.config.ObservabilityConfig;
import dev.denismasterherobrine.finale.arenaobservability.detector.FailureDetector;
import dev.denismasterherobrine.finale.arenaobservability.health.HealthSnapshot;
import dev.denismasterherobrine.finale.arenaobservability.health.ServerHealthEvaluator;
import dev.denismasterherobrine.finale.arenaobservability.metric.store.MetricStore;

import java.util.List;
import java.util.logging.Logger;

/**
 * Periodic task that runs every sample_interval_ticks.
 * Evaluates server health, runs failure detectors, updates gauges, and sends bridge snapshots.
 */
public class ObservabilityTicker implements Runnable {

    private final ObservabilityConfig config;
    private final ServerHealthEvaluator serverHealthEvaluator;
    private final List<FailureDetector> detectors;
    private final MetricStore store;
    private final MatchmakerBridge bridge;
    private final Logger logger;

    private long tickCount = 0;

    public ObservabilityTicker(ObservabilityConfig config, ServerHealthEvaluator serverHealthEvaluator,
                               List<FailureDetector> detectors, MetricStore store,
                               MatchmakerBridge bridge, Logger logger) {
        this.config = config;
        this.serverHealthEvaluator = serverHealthEvaluator;
        this.detectors = detectors;
        this.store = store;
        this.bridge = bridge;
        this.logger = logger;
    }

    @Override
    public void run() {
        try {
            tickCount++;

            HealthSnapshot snapshot = serverHealthEvaluator.evaluate();
            updateSessionGauges();

            for (FailureDetector detector : detectors) {
                try {
                    detector.tick();
                } catch (Exception e) {
                    logger.warning("[ObservabilityTicker] Detector error: " + e.getMessage());
                    store.incrementCounter("exception:arena-observability");
                }
            }

            if (config.isMatchmakerBridgeEnabled()) {
                long bridgeIntervalTicks = (long) config.getBatchIntervalSeconds() * 20;
                long sampleInterval = config.getSampleIntervalTicks();
                long bridgePeriod = Math.max(1, bridgeIntervalTicks / sampleInterval);

                if (tickCount % bridgePeriod == 0) {
                    bridge.sendSnapshot();
                }
            }
        } catch (Exception e) {
            logger.severe("[ObservabilityTicker] Tick error: " + e.getMessage());
            store.incrementCounter("exception:arena-observability");
        }
    }

    private void updateSessionGauges() {
        try {
            var runtimeClass = Class.forName("dev.denismasterherobrine.finale.arenaruntime.ArenaRuntimePlugin");
            var registryMethod = runtimeClass.getMethod("getSessionRegistry");
            var registry = registryMethod.invoke(null);
            if (registry != null) {
                var allMethod = registry.getClass().getMethod("getAllSessions");
                var sessions = (java.util.Collection<?>) allMethod.invoke(registry);
                store.setGauge("sessions_active", sessions.size());

                int playersInArenas = 0;
                for (Object session : sessions) {
                    var playersMethod = session.getClass().getMethod("getPlayers");
                    var players = (List<?>) playersMethod.invoke(session);
                    playersInArenas += players.size();
                }
                store.setGauge("players_in_arenas", playersInArenas);
            }
        } catch (Exception ignored) {
            store.setGauge("sessions_active", 0);
            store.setGauge("players_in_arenas", 0);
        }
    }
}
