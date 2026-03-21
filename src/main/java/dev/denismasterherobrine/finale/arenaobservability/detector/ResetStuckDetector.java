package dev.denismasterherobrine.finale.arenaobservability.detector;

import dev.denismasterherobrine.finale.arenaobservability.config.ObservabilityConfig;
import dev.denismasterherobrine.finale.arenaobservability.health.ReasonCode;
import dev.denismasterherobrine.finale.arenaobservability.incident.Incident;
import dev.denismasterherobrine.finale.arenaobservability.incident.IncidentCode;
import dev.denismasterherobrine.finale.arenaobservability.incident.IncidentManager;
import dev.denismasterherobrine.finale.arenaobservability.listener.ResetListener;

import java.util.EnumSet;
import java.util.Map;

public class ResetStuckDetector implements FailureDetector {

    private final ObservabilityConfig config;
    private final IncidentManager incidentManager;
    private final ResetListener resetListener;

    public ResetStuckDetector(ObservabilityConfig config, IncidentManager incidentManager, ResetListener resetListener) {
        this.config = config;
        this.incidentManager = incidentManager;
        this.resetListener = resetListener;
    }

    @Override
    public void tick() {
        if (!config.isResetStuckEnabled()) return;

        long now = System.currentTimeMillis();
        long timeoutMs = config.getResetTimeoutSeconds() * 1000L;

        for (Map.Entry<String, Long> entry : resetListener.getResetStartTimes().entrySet()) {
            String arenaId = entry.getKey();
            long startMs = entry.getValue();

            if (now - startMs > timeoutMs && !incidentManager.hasActive(IncidentCode.RESET_STUCK, arenaId)) {
                incidentManager.raise(
                        IncidentCode.RESET_STUCK,
                        Incident.Severity.CRITICAL,
                        arenaId,
                        EnumSet.of(ReasonCode.RESET_STUCK),
                        Map.of("elapsed_ms", (double) (now - startMs))
                );
            }
        }
    }
}
