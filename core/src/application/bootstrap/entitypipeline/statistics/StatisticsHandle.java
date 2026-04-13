package application.bootstrap.entitypipeline.statistics;

import application.core.engine.HandlePackage;
import application.core.settings.EngineSetting;

public class StatisticsHandle extends HandlePackage {

    /*
     * Per-entity runtime statistics. Holds movement speeds, jump height, and
     * reach. No manager owns this — it lives directly on EntityInstance and
     * is initialized to engine defaults on creation.
     */

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
        this.walkSpeed = EngineSetting.DEFAULT_WALK_SPEED;
        this.movementSpeed = EngineSetting.DEFAULT_MOVEMENT_SPEED;
        this.sprintSpeed = EngineSetting.DEFAULT_SPRINT_SPEED;

        // Physics
        this.jumpHeight = EngineSetting.DEFAULT_JUMP_HEIGHT;

        // Interaction
        this.reach = EngineSetting.DEFAULT_REACH;
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