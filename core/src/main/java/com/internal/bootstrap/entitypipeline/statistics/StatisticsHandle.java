package com.internal.bootstrap.entitypipeline.statistics;

import com.internal.core.engine.HandlePackage;

public class StatisticsHandle extends HandlePackage {

    // Movement
    private float walkSpeed;
    private float movementSpeed;
    private float sprintSpeed;

    // Physics
    private float jumpHeight;

    // Interaction
    private float reach;

    // Internal \\

    @Override
    protected void create() {

        // Movement
        this.walkSpeed = 1.4f;
        this.movementSpeed = 3.3f;
        this.sprintSpeed = 7f;

        // Physics
        this.jumpHeight = 0.5f;

        // Interaction
        this.reach = 1f;
    }

    // Accessible \\

    public float getWalkSpeed() {
        return walkSpeed;
    }

    public void setWalkSpeed(float walkSpeed) {
        this.walkSpeed = walkSpeed;
    }

    public float getMovementSpeed() {
        return movementSpeed;
    }

    public void setMovementSpeed(float movementSpeed) {
        this.movementSpeed = movementSpeed;
    }

    public float getSprintSpeed() {
        return sprintSpeed;
    }

    public void setSprintSpeed(float sprintSpeed) {
        this.sprintSpeed = sprintSpeed;
    }

    public float getJumpHeight() {
        return jumpHeight;
    }

    public void setJumpHeight(float jumpHeight) {
        this.jumpHeight = jumpHeight;
    }

    public float getReach() {
        return reach;
    }

    public void setReach(float reach) {
        this.reach = reach;
    }
}