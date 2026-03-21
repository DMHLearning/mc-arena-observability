package dev.denismasterherobrine.finale.arenaobservability.listener;

import dev.denismasterherobrine.finale.arenaobservability.config.ObservabilityConfig;
import dev.denismasterherobrine.finale.arenaobservability.metric.store.MetricStore;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeathListener implements Listener {

    private final MetricStore store;
    private final ObservabilityConfig config;

    public PlayerDeathListener(MetricStore store, ObservabilityConfig config) {
        this.store = store;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        EntityDamageEvent lastDamage = event.getEntity().getLastDamageCause();
        String cause = lastDamage != null ? lastDamage.getCause().name() : "UNKNOWN";

        store.incrementCounter("death:" + cause);

        if (isAnomalous(lastDamage)) {
            store.incrementCounter("anomalous_death");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.Player) return;

        String worldName = event.getEntity().getWorld().getName();
        store.recordMobDeath(worldName);
    }

    private boolean isAnomalous(EntityDamageEvent lastDamage) {
        if (!config.isAnomalousDeathEnabled()) return false;

        double mspt = Bukkit.getAverageTickTime();
        if (mspt > config.getAnomalousDeathMsptThreshold()) {
            return true;
        }

        if (lastDamage != null && config.getSuspiciousCauses() != null) {
            return config.getSuspiciousCauses().contains(lastDamage.getCause());
        }

        return false;
    }
}
