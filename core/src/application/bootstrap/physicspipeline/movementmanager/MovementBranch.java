package application.bootstrap.physicspipeline.movementmanager;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.entity.EntityState;
import application.bootstrap.entitypipeline.entity.EntityStateHandle;
import application.bootstrap.entitypipeline.statistics.StatisticsHandle;
import application.bootstrap.entitypipeline.util.EntityInputHandle;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector3;

public class MovementBranch extends BranchPackage {

    /*
     * Computes horizontal movement displacement each frame from the entity's
     * EntityInputHandle. Smoothly accelerates toward target velocity via lerp.
     * Y axis is not touched — owned by GravityBranch on land/air and by
     * SwimBranch once genuinely submerged (see MovementManager). dragMultiplier
     * scales the *target* speed rather than the resulting displacement, so
     * accel/decel still ramps smoothly, just toward a lower terminal speed —
     * see SwimBranch for how that multiplier is derived from viscosity.
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

    void calculate(Vector3 movement, EntityInstance entity, float dragMultiplier, boolean swimming) {

        EntityStateHandle state = entity.getEntityStateHandle();
        StatisticsHandle stats = entity.getStatisticsHandle();
        EntityInputHandle input = entity.getEntityInputHandle();
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

        // Real-world speed scaled by movementScale and, in liquid, drag
        float speed = selectSpeed(swimming, state.getMovementState(), stats) * movementScale * dragMultiplier;
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

    private float selectSpeed(boolean swimming, EntityState state, StatisticsHandle stats) {

        if (swimming)
            return stats.getSwimSpeed();

        return switch (state) {
            case WALKING -> stats.getWalkSpeed();
            case MOVING -> stats.getMovementSpeed();
            case RUNNING -> stats.getSprintSpeed();
            case JUMPING, FALLING -> stats.getMovementSpeed();
            default -> stats.getMovementSpeed();
        };
    }
}