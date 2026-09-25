package application.bootstrap.entitypipeline.entity;

public enum EntityState {

    /*
     * Movement states for an entity. Used by EntityStateHandle to drive
     * animation, physics, and input response. A state may name a fallback,
     * whose clip EntityBuilder uses whenever an entity authors none for it.
     */

    IDLE(null),
    WALKING(null),
    MOVING(null),
    RUNNING(null),
    JUMPING(null),
    FALLING(null),
    SWIMMING(null),
    WADING(null),
    TREADING(SWIMMING),
    WADING_IDLE(IDLE),
    SHALLOW_WADING(WADING),
    WADING_RUNNING(WADING),
    WATER_JUMPING(JUMPING),
    WATER_LEAPING(WATER_JUMPING);

    // Internal
    private final EntityState fallback;

    // Constructor \\

    EntityState(EntityState fallback) {
        this.fallback = fallback;
    }

    // Accessible \\

    public EntityState getFallback() {
        return fallback;
    }

    public boolean hasFallback() {
        return fallback != null;
    }
}
