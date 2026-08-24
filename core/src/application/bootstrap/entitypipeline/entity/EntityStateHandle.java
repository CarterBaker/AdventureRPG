package application.bootstrap.entitypipeline.entity;

import engine.root.HandlePackage;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector3;

public class EntityStateHandle extends HandlePackage {

    /*
     * Per-entity runtime movement state. Holds current movement state, gravity
     * and horizontal velocity accumulators, jump start time, and the smoothed
     * cosmetic vertical ground offset NaturalGroundOffsetBranch derives from
     * whichever natural block currently sits beneath this entity's feet. No
     * manager owns this — it lives directly on EntityInstance.
     */

    // State
    private EntityState movementState;

    // Velocity
    private Vector3 gravityVelocity;
    private Vector2 horizontalVelocity;
    private long jumpStartTime;

    // Ground Offset — cosmetic only, never fed back into physics position
    private float groundOffset;

    // Internal \\

    @Override
    protected void create() {

        // State
        this.movementState = EntityState.IDLE;

        // Velocity
        this.gravityVelocity = new Vector3();
        this.horizontalVelocity = new Vector2();
        this.jumpStartTime = 0L;

        // Ground Offset
        this.groundOffset = 0f;
    }

    // Accessible \\

    public EntityState getMovementState() {
        return movementState;
    }

    public void setMovementState(EntityState movementState) {
        this.movementState = movementState;
    }

    public Vector3 getGravityVelocity() {
        return gravityVelocity;
    }

    public Vector2 getHorizontalVelocity() {
        return horizontalVelocity;
    }

    public long getJumpStartTime() {
        return jumpStartTime;
    }

    public void setJumpStartTime(long jumpStartTime) {
        this.jumpStartTime = jumpStartTime;
    }

    public float getGroundOffset() {
        return groundOffset;
    }

    public void setGroundOffset(float groundOffset) {
        this.groundOffset = groundOffset;
    }

    // Utility \\

    public boolean isGrounded() {
        return movementState != EntityState.JUMPING
                && movementState != EntityState.FALLING
                && movementState != EntityState.SWIMMING;
    }
}