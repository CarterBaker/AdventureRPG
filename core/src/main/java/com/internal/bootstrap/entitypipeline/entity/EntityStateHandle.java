package com.internal.bootstrap.entitypipeline.entity;

import com.internal.core.engine.HandlePackage;
import com.internal.core.util.mathematics.vectors.Vector2;

public class EntityStateHandle extends HandlePackage {

    // State — what the entity is currently doing
    private EntityState movementState;

    // Velocity — owned here because velocity and state are directly coupled
    private float verticalVelocity;
    private Vector2 horizontalVelocity;
    private long jumpStartTime;

    // Constructor \\

    public void constructor() {
        this.movementState = EntityState.IDLE;
        this.verticalVelocity = 0f;
        this.horizontalVelocity = new Vector2();
        this.jumpStartTime = 0L;
    }

    // Getters \\

    public EntityState getMovementState() {
        return movementState;
    }

    public float getVerticalVelocity() {
        return verticalVelocity;
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

    public void setVerticalVelocity(float verticalVelocity) {
        this.verticalVelocity = verticalVelocity;
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