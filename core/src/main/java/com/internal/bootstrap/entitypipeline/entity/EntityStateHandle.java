package com.internal.bootstrap.entitypipeline.entity;

import com.internal.core.engine.HandlePackage;
import com.internal.core.util.mathematics.vectors.Vector2;
import com.internal.core.util.mathematics.vectors.Vector3;

public class EntityStateHandle extends HandlePackage {

    // State
    private EntityState movementState;

    // Velocity
    private Vector3 gravityVelocity;
    private Vector2 horizontalVelocity;
    private long jumpStartTime;

    // Internal \\

    @Override
    protected void create() {

        // State
        this.movementState = EntityState.IDLE;

        // Velocity
        this.gravityVelocity = new Vector3();
        this.horizontalVelocity = new Vector2();
        this.jumpStartTime = 0L;
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

    // Utility \\

    public boolean isGrounded() {
        return movementState != EntityState.JUMPING
                && movementState != EntityState.FALLING;
    }
}