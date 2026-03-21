package dev.denismasterherobrine.finale.arenaobservability.detector;

/**
 * Interface for periodic failure detectors. Called every tick cycle (e.g. every 5 seconds).
 */
public interface FailureDetector {

    void tick();
}
