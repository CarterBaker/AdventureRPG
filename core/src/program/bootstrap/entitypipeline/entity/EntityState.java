package program.bootstrap.entitypipeline.entity;

public enum EntityState {

    /*
     * Movement states for an entity. Used by EntityStateHandle to drive
     * animation, physics, and input response.
     */

    IDLE,
    WALKING,
    MOVING,
    RUNNING,
    JUMPING,
    FALLING
}