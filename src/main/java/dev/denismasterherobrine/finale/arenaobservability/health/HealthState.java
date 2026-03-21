package dev.denismasterherobrine.finale.arenaobservability.health;

public enum HealthState {
    HEALTHY,
    DEGRADED,
    CRITICAL,
    FAILED;

    public boolean isWorseThan(HealthState other) {
        return this.ordinal() > other.ordinal();
    }

    public HealthState worse(HealthState other) {
        return this.ordinal() >= other.ordinal() ? this : other;
    }
}
