package application.bootstrap.entitypipeline.entity;

public enum EntityState {

    /*
     * Movement states for an entity. Used by EntityStateHandle to drive
     * physics and input response, and by an entity's animation tree, which
     * maps each state by its lower-case name to the node it plays.
     */

    IDLE,
    WALKING,
    MOVING,
    RUNNING,
    JUMPING,
    FALLING,
    SWIMMING,
    TREADING,
    UNDERWATER_SWIMMING,
    UNDERWATER_TREADING,
    DIVING,
    SURFACING,
    WADING,
    WADING_IDLE,
    WADING_RUNNING,
    SHALLOW_WADING,
    SHALLOW_WADING_IDLE,
    SHALLOW_WADING_RUNNING,
    WATER_JUMPING,
    WATER_LEAPING;

    // Values
    public static final EntityState[] VALUES = values();
}
