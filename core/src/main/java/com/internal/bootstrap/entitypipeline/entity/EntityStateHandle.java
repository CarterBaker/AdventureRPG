package com.internal.bootstrap.entitypipeline.entity;

import com.internal.core.engine.HandlePackage;
import com.internal.core.util.mathematics.vectors.Vector2;
import com.internal.core.util.mathematics.vectors.Vector3;

public class EntityStateHandle extends HandlePackage {

    // State
    private EntityState movementState;

    // Velocity
    private Vector3 gravityVelocity; // accumulates along gravity axes
    private Vector2 horizontalVelocity; // accumulates along movement plane
    private long jumpStartTime;

    // Constructor \\

    public void constructor() {
        this.movementState = EntityState.IDLE;
        this.gravityVelocity = new Vector3();
        this.horizontalVelocity = new Vector2();
        this.jumpStartTime = 0L;
    }

    // Getters \\

    public EntityState getMovementState() {
        return movementState;
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

    // Setters \\

    public void setMovementState(EntityState movementState) {
        this.movementState = movementState;
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