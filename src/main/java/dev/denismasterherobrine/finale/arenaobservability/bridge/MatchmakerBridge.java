package dev.denismasterherobrine.finale.arenaobservability.bridge;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import dev.denismasterherobrine.finale.arenaobservability.runtime.RuntimeFlags;
import dev.denismasterherobrine.finale.arenaobservability.config.ObservabilityConfig;
import dev.denismasterherobrine.finale.arenaobservability.health.ArenaHealthEvaluator;
import dev.denismasterherobrine.finale.arenaobservability.health.HealthState;
import dev.denismasterherobrine.finale.arenaobservability.health.ServerHealthEvaluator;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/**
 * Sends health-aware ARENA_STATUS messages to arena-matchmaker on Velocity
 * via the `arenamatchmaker:status` plugin messaging channel.
 *
 * Wire format matches PluginMessageHandler.handleArenaStatus in arena-matchmaker:
 * <pre>
 *   byte    ARENA_STATUS (0x03)
 *   UTF     arenaId
 *   UTF     serverId
 *   UTF     mode
 *   int     currentPlayers
 *   int     maxPlayers
 *   UTF     healthStateName  ("HEALTHY" | "DEGRADED" | "CRITICAL" | "FAILED")
 *   int     flagCount
 *   UTF[]   flagNames
 *   long    ttlMs
 * </pre>
 */
public class MatchmakerBridge {

    public static final String CHANNEL = "arenamatchmaker:status";
    private static final byte ARENA_STATUS = 0x03;

    private final JavaPlugin plugin;
    private final ObservabilityConfig config;
    private final ServerHealthEvaluator serverHealth;
    private final ArenaHealthEvaluator arenaHealth;
    private final Logger logger;

    private HealthState lastSentState = null;

    public MatchmakerBridge(JavaPlugin plugin, ObservabilityConfig config,
                            ServerHealthEvaluator serverHealth, ArenaHealthEvaluator arenaHealth, Logger logger) {
        this.plugin = plugin;
        this.config = config;
        this.serverHealth = serverHealth;
        this.arenaHealth = arenaHealth;
        this.logger = logger;
    }

    /**
     * Called periodically (every batch_interval_seconds) and on state changes.
     */
    public void sendSnapshot() {
        if (RuntimeFlags.bridgeDrop) return;
        Collection<? extends Player> online = Bukkit.getOnlinePlayers();
        if (online.isEmpty()) return;

        Player carrier = online.iterator().next();
        if (!carrier.isOnline()) return;

        HealthState serverState = serverHealth.getCurrentState();
        HealthState arenaState = arenaHealth.getArenaHealth(config.getMatchmakerArenaId());
        HealthState combined = serverState.worse(arenaState);

        int currentPlayers = 0;
        try {
            var runtimeClass = Class.forName("dev.denismasterherobrine.finale.arenaruntime.ArenaRuntimePlugin");
            var registryMethod = runtimeClass.getMethod("getSessionRegistry");
            var registry = registryMethod.invoke(null);
            if (registry != null) {
                var allMethod = registry.getClass().getMethod("getAllSessions");
                var sessions = (Collection<?>) allMethod.invoke(registry);
                for (Object session : sessions) {
                    var playersMethod = session.getClass().getMethod("getPlayers");
                    var players = (List<?>) playersMethod.invoke(session);
                    currentPlayers += players.size();
                }
            }
        } catch (Exception ignored) {
            currentPlayers = Bukkit.getOnlinePlayers().size();
        }

        List<String> flags = new ArrayList<>();
        if (currentPlayers >= config.getMatchmakerMaxPlayers()) {
            flags.add("FULL");
        }
        if (combined == HealthState.CRITICAL || combined == HealthState.FAILED) {
            flags.add("DRAINING");
        }

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeByte(ARENA_STATUS);
        out.writeUTF(config.getMatchmakerArenaId());
        out.writeUTF(config.getMatchmakerServerId());
        out.writeUTF(config.getMatchmakerMode());
        out.writeInt(currentPlayers);
        out.writeInt(config.getMatchmakerMaxPlayers());
        out.writeUTF(combined.name());
        out.writeInt(flags.size());
        for (String flag : flags) {
            out.writeUTF(flag);
        }
        out.writeLong(config.getMatchmakerTtlMs());

        carrier.sendPluginMessage(plugin, CHANNEL, out.toByteArray());

        if (lastSentState != combined) {
            logger.info("[MatchmakerBridge] Sent status: " + combined.name() +
                    " players=" + currentPlayers + " flags=" + flags);
            lastSentState = combined;
        }
    }
}
