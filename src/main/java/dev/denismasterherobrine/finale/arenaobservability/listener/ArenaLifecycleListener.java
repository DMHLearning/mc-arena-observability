package dev.denismasterherobrine.finale.arenaobservability.listener;

import dev.denismasterherobrine.finale.arenaobservability.metric.store.MetricStore;
import dev.denismasterherobrine.finale.arenaruntime.event.*;
import dev.denismasterherobrine.finale.arenaruntime.game.ArenaState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ArenaLifecycleListener implements Listener {

    private final MetricStore store;

    public ArenaLifecycleListener(MetricStore store) {
        this.store = store;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStateChanged(ArenaStateChangedEvent event) {
        if (event.getNewState() == ArenaState.RESET || event.getNewState() == ArenaState.IDLE) {
            store.removeArena(event.getArenaId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSessionStarted(SessionStartedEvent event) {
        store.incrementCounter("session_start:" + event.getArenaId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSessionEnded(SessionEndedEvent event) {
        store.incrementCounter("session_end:" + event.getArenaId() + ":" + event.getReason().name());

        if (event.getReason() == SessionEndedEvent.EndReason.PREPARATION_FAILED
                || event.getReason() == SessionEndedEvent.EndReason.SUPERVISOR_SOFT_FAIL) {
            store.incrementCounter("soft_fail:" + event.getArenaId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWaveStarted(WaveStartedEvent event) {
        store.setGauge("wave_index:" + event.getArenaId(), event.getWaveIndex());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWaveCompleted(WaveCompletedEvent event) {
        store.recordWaveDuration(event.getArenaId(), event.getDurationMs());
    }
}
