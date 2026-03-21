package dev.denismasterherobrine.finale.arenaobservability.detector;

import dev.denismasterherobrine.finale.arenaobservability.config.ObservabilityConfig;
import dev.denismasterherobrine.finale.arenaobservability.health.ReasonCode;
import dev.denismasterherobrine.finale.arenaobservability.incident.Incident;
import dev.denismasterherobrine.finale.arenaobservability.incident.IncidentCode;
import dev.denismasterherobrine.finale.arenaobservability.incident.IncidentManager;
import dev.denismasterherobrine.finale.arenaobservability.metric.store.MetricStore;
import dev.denismasterherobrine.finale.arenaruntime.ArenaRuntimePlugin;
import dev.denismasterherobrine.finale.arenaruntime.game.ArenaState;
import dev.denismasterherobrine.finale.arenaruntime.game.session.ArenaSession;
import dev.denismasterherobrine.finale.arenaruntime.game.wave.WaveManager;

import java.util.EnumSet;
import java.util.Map;

public class WaveStuckDetector implements FailureDetector {

    private final ObservabilityConfig config;
    private final MetricStore store;
    private final IncidentManager incidentManager;

    public WaveStuckDetector(ObservabilityConfig config, MetricStore store, IncidentManager incidentManager) {
        this.config = config;
        this.store = store;
        this.incidentManager = incidentManager;
    }

    @Override
    public void tick() {
        if (!config.isWaveStuckEnabled()) return;

        try {
            var registry = ArenaRuntimePlugin.getSessionRegistry();
            if (registry == null) return;

            for (ArenaSession session : registry.getAllSessions()) {
                checkSession(session);
            }
        } catch (NoClassDefFoundError | Exception ignored) {
            // ArenaRuntime not loaded
        }
    }

    private void checkSession(ArenaSession session) {
        if (session.getState() != ArenaState.RUNNING) {
            if (incidentManager.hasActive(IncidentCode.WAVE_STUCK, session.getArenaId())) {
                incidentManager.resolve(IncidentCode.WAVE_STUCK, session.getArenaId());
            }
            return;
        }

        WaveManager wm = session.getWaveManager();
        if (wm == null || wm.isFinished() || wm.isAtCheckpoint()) return;

        long waveStartMs = wm.getWaveStartTimeMs();
        if (waveStartMs == 0) return;

        long elapsed = System.currentTimeMillis() - waveStartMs;
        long maxDurationMs = config.getMaxWaveDurationSeconds() * 1000L;

        if (elapsed < maxDurationMs) {
            if (incidentManager.hasActive(IncidentCode.WAVE_STUCK, session.getArenaId())) {
                incidentManager.resolve(IncidentCode.WAVE_STUCK, session.getArenaId());
            }
            return;
        }

        long lastMobDeath = store.getLastMobDeathMs(session.getArenaId());
        long noProgressMs = config.getNoProgressSeconds() * 1000L;
        boolean noProgress = lastMobDeath == 0 || (System.currentTimeMillis() - lastMobDeath) > noProgressMs;

        if (noProgress && !incidentManager.hasActive(IncidentCode.WAVE_STUCK, session.getArenaId())) {
            incidentManager.raise(
                    IncidentCode.WAVE_STUCK,
                    Incident.Severity.WARNING,
                    session.getArenaId(),
                    EnumSet.of(ReasonCode.WAVE_STUCK),
                    Map.of("wave_index", (double) wm.getCurrentWave(),
                            "elapsed_ms", (double) elapsed,
                            "alive_mobs", (double) wm.getAliveMobCount())
            );
        }
    }
}
