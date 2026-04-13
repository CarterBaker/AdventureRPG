package application.bootstrap.physicspipeline.movementmanager;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.entity.EntityState;
import application.bootstrap.entitypipeline.entity.EntityStateHandle;
import application.bootstrap.entitypipeline.statistics.StatisticsHandle;
import application.bootstrap.inputpipeline.input.InputHandle;
import application.core.engine.BranchPackage;
import application.core.settings.EngineSetting;
import application.core.util.mathematics.vectors.Vector2;
import application.core.util.mathematics.vectors.Vector3;

public class MovementBranch extends BranchPackage {

    /*
     * Computes horizontal movement displacement each frame from the entity's
     * InputHandle. Smoothly accelerates toward target velocity via lerp.
     * Y axis is not touched — owned entirely by GravityBranch.
     */

    // Settings
    private float movementAcceleration;
    private float movementScale;

    // Cached Vectors
    private Vector3 forward;
    private Vector3 right;

    // Internal \\

    @Override
    protected void create() {

        // Settings
        this.movementAcceleration = EngineSetting.MOVEMENT_ACCELERATION;
        this.movementScale = EngineSetting.MOVEMENT_SCALE;

        // Cached Vectors
        this.forward = new Vector3();
        this.right = new Vector3();
    }

    // Movement \\

    void calculate(Vector3 movement, EntityInstance entity) {

        EntityStateHandle state = entity.getEntityStateHandle();
        StatisticsHandle stats = entity.getStatisticsHandle();
        InputHandle input = entity.getInputHandle();
        Vector2 vel = state.getHorizontalVelocity();
        Vector3 facing = input.getFacingDirection();
        float delta = internal.getDeltaTime();

        // Forward — flatten pitch to horizontal plane
        forward.x = facing.x;
        forward.y = 0f;
        forward.z = facing.z;
        forward.normalize();

        // Right — perpendicular to forward
        right.x = -forward.z;
        right.y = 0f;
        right.z = forward.x;

        int inputX = input.getHorizontalX();
        int inputZ = input.getHorizontalZ();

        // Target direction from input
        float targetX = forward.x * inputZ + right.x * inputX;
        float targetZ = forward.z * inputZ + right.z * inputX;

        // Normalize target direction
        float len = (float) Math.sqrt(targetX * targetX + targetZ * targetZ);

        if (len > 0f) {
            targetX /= len;
            targetZ /= len;
        }

        // Real-world speed scaled by movementScale for game feel
        float speed = selectSpeed(state.getMovementState(), stats) * movementScale;
        targetX *= speed;
        targetZ *= speed;

        // Lerp toward target — smooth acceleration ramp
        float lerpFactor = Math.min(1f, delta * movementAcceleration);
        vel.x += (targetX - vel.x) * lerpFactor;
        vel.y += (targetZ - vel.y) * lerpFactor;

        // Write to movement — Y untouched
        movement.x = vel.x * delta;
        movement.z = vel.y * delta;
    }

    // Speed \\

    private float selectSpeed(EntityState state, StatisticsHandle stats) {
        return switch (state) {
            case WALKING -> stats.getWalkSpeed();
            case MOVING -> stats.getMovementSpeed();
            case RUNNING -> stats.getSprintSpeed();
            case JUMPING, FALLING -> stats.getMovementSpeed();
            default -> stats.getMovementSpeed();
        };
    }
}