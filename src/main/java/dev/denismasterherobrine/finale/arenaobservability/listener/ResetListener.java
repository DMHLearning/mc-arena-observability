package dev.denismasterherobrine.finale.arenaobservability.listener;

import dev.denismasterherobrine.arenaworldmanager.event.ArenaResetCompletedEvent;
import dev.denismasterherobrine.arenaworldmanager.event.ArenaResetFailedEvent;
import dev.denismasterherobrine.arenaworldmanager.event.ArenaResetStartedEvent;
import dev.denismasterherobrine.finale.arenaobservability.metric.store.MetricStore;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ResetListener implements Listener {

    private final MetricStore store;
    private final Map<String, Long> resetStartTimes = new ConcurrentHashMap<>();

    public ResetListener(MetricStore store) {
        this.store = store;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onResetStarted(ArenaResetStartedEvent event) {
        resetStartTimes.put(event.getArenaId(), System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onResetCompleted(ArenaResetCompletedEvent event) {
        resetStartTimes.remove(event.getArenaId());
        store.incrementCounter("reset_success:" + event.getArenaId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onResetFailed(ArenaResetFailedEvent event) {
        resetStartTimes.remove(event.getArenaId());
        store.incrementCounter("reset_fail:" + event.getArenaId());
        store.incrementCounter("exception:arena-world-manager");
    }

    public Map<String, Long> getResetStartTimes() {
        return resetStartTimes;
    }
}
