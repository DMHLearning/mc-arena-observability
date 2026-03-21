package dev.denismasterherobrine.finale.arenaobservability;

import dev.denismasterherobrine.finale.arenaobservability.bridge.MatchmakerBridge;
import dev.denismasterherobrine.finale.arenaobservability.config.ObservabilityConfig;
import dev.denismasterherobrine.finale.arenaobservability.detector.*;
import dev.denismasterherobrine.finale.arenaobservability.health.ArenaHealthEvaluator;
import dev.denismasterherobrine.finale.arenaobservability.health.ServerHealthEvaluator;
import dev.denismasterherobrine.finale.arenaobservability.incident.IncidentManager;
import dev.denismasterherobrine.finale.arenaobservability.listener.ArenaLifecycleListener;
import dev.denismasterherobrine.finale.arenaobservability.listener.PlayerDeathListener;
import dev.denismasterherobrine.finale.arenaobservability.listener.ResetListener;
import dev.denismasterherobrine.finale.arenaobservability.metric.ArenaMetricsCollection;
import dev.denismasterherobrine.finale.arenaobservability.metric.store.MetricStore;
import dev.denismasterherobrine.finale.arenaobservability.scheduler.ObservabilityTicker;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ArenaObservabilityPlugin extends JavaPlugin {

    private ObservabilityConfig observabilityConfig;
    private MetricStore metricStore;
    private ArenaMetricsCollection metricsCollection;
    private ResetListener resetListener;

    @Override
    public void onEnable() {
        Logger log = getLogger();

        saveDefaultConfig();
        observabilityConfig = new ObservabilityConfig();
        observabilityConfig.load(getConfig());

        metricStore = new MetricStore();
        ArenaHealthEvaluator arenaHealthEvaluator = new ArenaHealthEvaluator();
        ServerHealthEvaluator serverHealthEvaluator = new ServerHealthEvaluator(observabilityConfig);
        IncidentManager incidentManager = new IncidentManager(observabilityConfig, arenaHealthEvaluator, log);

        // --- Listeners ---
        boolean arenaRuntimeAvailable = isPluginPresent("ArenaRuntime");
        boolean worldManagerAvailable = isPluginPresent("ArenaWorldManagerPlugin");

        if (arenaRuntimeAvailable) {
            ArenaLifecycleListener lifecycleListener = new ArenaLifecycleListener(metricStore);
            Bukkit.getPluginManager().registerEvents(lifecycleListener, this);
            log.info("ArenaRuntime detected — lifecycle listeners registered.");
        } else {
            log.warning("ArenaRuntime not found — arena lifecycle metrics will be unavailable.");
        }

        resetListener = new ResetListener(metricStore);
        if (worldManagerAvailable) {
            Bukkit.getPluginManager().registerEvents(resetListener, this);
            log.info("ArenaWorldManager detected — reset listeners registered.");
        } else {
            log.warning("ArenaWorldManager not found — reset metrics will be unavailable.");
        }

        PlayerDeathListener deathListener = new PlayerDeathListener(metricStore, observabilityConfig);
        Bukkit.getPluginManager().registerEvents(deathListener, this);

        // --- Detectors ---
        List<FailureDetector> detectors = new ArrayList<>();
        detectors.add(new PerformanceDetector(serverHealthEvaluator, incidentManager));

        if (arenaRuntimeAvailable) {
            detectors.add(new WaveStuckDetector(observabilityConfig, metricStore, incidentManager));
        }
        detectors.add(new ResetStuckDetector(observabilityConfig, incidentManager, resetListener));
        detectors.add(new AnomalousDeathDetector(metricStore));

        // --- Bridge ---
        MatchmakerBridge bridge = null;
        if (observabilityConfig.isMatchmakerBridgeEnabled()) {
            getServer().getMessenger().registerOutgoingPluginChannel(this, MatchmakerBridge.CHANNEL);
            bridge = new MatchmakerBridge(this, observabilityConfig, serverHealthEvaluator, arenaHealthEvaluator, log);
            log.info("MatchmakerBridge enabled — status will be sent to Velocity.");
        }

        // --- UnifiedMetrics registration ---
        metricsCollection = new ArenaMetricsCollection(metricStore, serverHealthEvaluator, incidentManager);
        if (observabilityConfig.isUnifiedMetricsEnabled()) {
            try {
                var provider = Class.forName("dev.cubxity.plugins.metrics.api.UnifiedMetricsProvider");
                var getMethod = provider.getMethod("get");
                var api = getMethod.invoke(null);
                var metricsManagerMethod = api.getClass().getMethod("getMetricsManager");
                var metricsManager = metricsManagerMethod.invoke(api);
                var registerMethod = metricsManager.getClass().getMethod("registerCollection",
                        Class.forName("dev.cubxity.plugins.metrics.api.metric.collector.CollectorCollection"));
                registerMethod.invoke(metricsManager, metricsCollection);
                log.info("UnifiedMetrics detected — custom arena metrics registered.");
            } catch (Exception e) {
                log.warning("UnifiedMetrics not available — metrics will not be exported. (" + e.getMessage() + ")");
            }
        }

        // --- Scheduler ---
        ObservabilityTicker ticker = new ObservabilityTicker(
                observabilityConfig, serverHealthEvaluator, detectors, metricStore,
                bridge != null ? bridge : new NoOpBridge(), log);

        Bukkit.getScheduler().runTaskTimer(this, ticker,
                observabilityConfig.getSampleIntervalTicks(),
                observabilityConfig.getSampleIntervalTicks());

        log.info("ArenaObservability v" + getDescription().getVersion() + " enabled — "
                + detectors.size() + " detectors active, interval=" + observabilityConfig.getSampleIntervalTicks() + " ticks.");
    }

    @Override
    public void onDisable() {
        if (observabilityConfig != null && observabilityConfig.isUnifiedMetricsEnabled() && metricsCollection != null) {
            try {
                var provider = Class.forName("dev.cubxity.plugins.metrics.api.UnifiedMetricsProvider");
                var getMethod = provider.getMethod("get");
                var api = getMethod.invoke(null);
                var metricsManagerMethod = api.getClass().getMethod("getMetricsManager");
                var metricsManager = metricsManagerMethod.invoke(api);
                var unregisterMethod = metricsManager.getClass().getMethod("unregisterCollection",
                        Class.forName("dev.cubxity.plugins.metrics.api.metric.collector.CollectorCollection"));
                unregisterMethod.invoke(metricsManager, metricsCollection);
            } catch (Exception ignored) {}
        }

        if (observabilityConfig != null && observabilityConfig.isMatchmakerBridgeEnabled()) {
            getServer().getMessenger().unregisterOutgoingPluginChannel(this, MatchmakerBridge.CHANNEL);
        }

        getLogger().info("ArenaObservability disabled.");
    }

    private boolean isPluginPresent(String name) {
        return Bukkit.getPluginManager().getPlugin(name) != null;
    }

    /**
     * No-op bridge used when matchmaker bridge is disabled.
     */
    private static class NoOpBridge extends MatchmakerBridge {
        NoOpBridge() {
            super(null, null, null, null, null);
        }

        @Override
        public void sendSnapshot() {
            // do nothing
        }
    }
}
