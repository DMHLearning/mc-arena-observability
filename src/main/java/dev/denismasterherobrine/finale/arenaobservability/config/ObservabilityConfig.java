package dev.denismasterherobrine.finale.arenaobservability.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.ArrayList;
import java.util.List;

public class ObservabilityConfig {

    private int sampleIntervalTicks;

    private int degradeEnterSeconds;
    private int degradeExitSeconds;
    private int criticalEnterSeconds;
    private int criticalExitSeconds;

    private double tpsDegradedBelow;
    private double tpsCriticalBelow;
    private double msptDegradedAbove;
    private double msptCriticalAbove;

    private boolean waveStuckEnabled;
    private int maxWaveDurationSeconds;
    private int noProgressSeconds;

    private boolean resetStuckEnabled;
    private int resetTimeoutSeconds;

    private boolean anomalousDeathEnabled;
    private double anomalousDeathMsptThreshold;
    private List<EntityDamageEvent.DamageCause> suspiciousCauses;

    private boolean unifiedMetricsEnabled;
    private boolean matchmakerBridgeEnabled;
    private int batchIntervalSeconds;
    private int incidentPerArenaPerMinute;

    private String matchmakerServerId;
    private String matchmakerArenaId;
    private String matchmakerMode;
    private int matchmakerMaxPlayers;
    private long matchmakerTtlMs;

    public void load(FileConfiguration cfg) {
        ConfigurationSection health = cfg.getConfigurationSection("health");
        if (health != null) {
            sampleIntervalTicks = health.getInt("sample_interval_ticks", 100);

            ConfigurationSection hyst = health.getConfigurationSection("hysteresis");
            if (hyst != null) {
                degradeEnterSeconds = hyst.getInt("degrade_enter_seconds", 10);
                degradeExitSeconds = hyst.getInt("degrade_exit_seconds", 20);
                criticalEnterSeconds = hyst.getInt("critical_enter_seconds", 5);
                criticalExitSeconds = hyst.getInt("critical_exit_seconds", 30);
            }

            ConfigurationSection thresholds = health.getConfigurationSection("thresholds");
            if (thresholds != null) {
                tpsDegradedBelow = thresholds.getDouble("tps.degraded_below", 19.93);
                tpsCriticalBelow = thresholds.getDouble("tps.critical_below", 15.0);
                msptDegradedAbove = thresholds.getDouble("mspt.degraded_above", 40.0);
                msptCriticalAbove = thresholds.getDouble("mspt.critical_above", 62.5);
            }
        }

        ConfigurationSection det = cfg.getConfigurationSection("detectors");
        if (det != null) {
            ConfigurationSection ws = det.getConfigurationSection("wave_stuck");
            if (ws != null) {
                waveStuckEnabled = ws.getBoolean("enabled", true);
                maxWaveDurationSeconds = ws.getInt("max_wave_duration_seconds", 180);
                noProgressSeconds = ws.getInt("no_progress_seconds", 30);
            }

            ConfigurationSection rs = det.getConfigurationSection("reset_stuck");
            if (rs != null) {
                resetStuckEnabled = rs.getBoolean("enabled", true);
                resetTimeoutSeconds = rs.getInt("reset_timeout_seconds", 60);
            }

            ConfigurationSection ad = det.getConfigurationSection("anomalous_death");
            if (ad != null) {
                anomalousDeathEnabled = ad.getBoolean("enabled", true);
                anomalousDeathMsptThreshold = ad.getDouble("mspt_threshold", 45.0);
                suspiciousCauses = new ArrayList<>();
                for (String cause : ad.getStringList("suspicious_causes")) {
                    try {
                        suspiciousCauses.add(EntityDamageEvent.DamageCause.valueOf(cause));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }

        ConfigurationSection export = cfg.getConfigurationSection("export");
        if (export != null) {
            unifiedMetricsEnabled = export.getBoolean("unifiedmetrics.enabled", true);

            ConfigurationSection mb = export.getConfigurationSection("matchmaker_bridge");
            if (mb != null) {
                matchmakerBridgeEnabled = mb.getBoolean("enabled", true);
                batchIntervalSeconds = mb.getInt("batch_interval_seconds", 5);

                ConfigurationSection rl = mb.getConfigurationSection("rate_limit");
                if (rl != null) {
                    incidentPerArenaPerMinute = rl.getInt("incident_per_arena_per_minute", 6);
                }
            }
        }

        ConfigurationSection mm = cfg.getConfigurationSection("matchmaker");
        if (mm != null) {
            matchmakerServerId = mm.getString("server-id", "paper-arena");
            matchmakerArenaId = mm.getString("arena-id", "solo_arena_01");
            matchmakerMode = mm.getString("mode", "solo");
            matchmakerMaxPlayers = mm.getInt("max-players", 1);
            matchmakerTtlMs = mm.getLong("ttl-ms", 315360000000L);
        }
    }

    public int getSampleIntervalTicks() { return sampleIntervalTicks; }

    public int getDegradeEnterSeconds() { return degradeEnterSeconds; }
    public int getDegradeExitSeconds() { return degradeExitSeconds; }
    public int getCriticalEnterSeconds() { return criticalEnterSeconds; }
    public int getCriticalExitSeconds() { return criticalExitSeconds; }

    public double getTpsDegradedBelow() { return tpsDegradedBelow; }
    public double getTpsCriticalBelow() { return tpsCriticalBelow; }
    public double getMsptDegradedAbove() { return msptDegradedAbove; }
    public double getMsptCriticalAbove() { return msptCriticalAbove; }

    public boolean isWaveStuckEnabled() { return waveStuckEnabled; }
    public int getMaxWaveDurationSeconds() { return maxWaveDurationSeconds; }
    public int getNoProgressSeconds() { return noProgressSeconds; }

    public boolean isResetStuckEnabled() { return resetStuckEnabled; }
    public int getResetTimeoutSeconds() { return resetTimeoutSeconds; }

    public boolean isAnomalousDeathEnabled() { return anomalousDeathEnabled; }
    public double getAnomalousDeathMsptThreshold() { return anomalousDeathMsptThreshold; }
    public List<EntityDamageEvent.DamageCause> getSuspiciousCauses() { return suspiciousCauses; }

    public boolean isUnifiedMetricsEnabled() { return unifiedMetricsEnabled; }
    public boolean isMatchmakerBridgeEnabled() { return matchmakerBridgeEnabled; }
    public int getBatchIntervalSeconds() { return batchIntervalSeconds; }
    public int getIncidentPerArenaPerMinute() { return incidentPerArenaPerMinute; }

    public String getMatchmakerServerId() { return matchmakerServerId; }
    public String getMatchmakerArenaId() { return matchmakerArenaId; }
    public String getMatchmakerMode() { return matchmakerMode; }
    public int getMatchmakerMaxPlayers() { return matchmakerMaxPlayers; }
    public long getMatchmakerTtlMs() { return matchmakerTtlMs; }
}
