package dev.denismasterherobrine.finale.arenaobservability.runtime;

/**
 * Process-wide runtime flags observed by the observability subsystem (MetricStore writes,
 * MatchmakerBridge.sendSnapshot). All fields are volatile so external chaos engines can
 * flip them from any thread.
 *
 * <p>Lives in a non-chaos package so chaos plugins can manipulate it without the source
 * plugin having any compile-time dependency on chaos infrastructure.
 */
public final class RuntimeFlags {

    private RuntimeFlags() {}

    /** If true, MetricStore silently drops every counter/gauge write. */
    public static volatile boolean metricBlackhole = false;
    /** If true, MatchmakerBridge.sendSnapshot returns early. */
    public static volatile boolean bridgeDrop = false;
    /** If &gt; 0, MetricStore amplifies counter/gauge writes by this factor (storm). */
    public static volatile int metricStormFactor = 0;

    public static void reset() {
        metricBlackhole = false;
        bridgeDrop = false;
        metricStormFactor = 0;
    }
}
