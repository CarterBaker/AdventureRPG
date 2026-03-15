package com.internal.bootstrap.physicspipeline.movementmanager;

import com.internal.bootstrap.entitypipeline.entity.EntityInstance;
import com.internal.bootstrap.entitypipeline.entity.EntityState;
import com.internal.bootstrap.entitypipeline.entity.EntityStateHandle;
import com.internal.bootstrap.entitypipeline.statistics.StatisticsHandle;
import com.internal.core.engine.BranchPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.mathematics.vectors.Vector2;
import com.internal.core.util.mathematics.vectors.Vector3;
import com.internal.core.util.mathematics.vectors.Vector3Int;

public class MovementBranch extends BranchPackage {

    /*
     * Computes horizontal movement displacement each frame from directional input
     * and camera orientation. Smoothly accelerates toward target velocity via lerp.
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

    /*
     * Horizontal movement only — Y is owned by GravityBranch.
     * Real-world speeds from StatisticsHandle are scaled by movementScale for
     * game feel. Does not know or care if input came from player or AI.
     */
    void calculate(
            Vector3Int input,
            Vector3 cameraDirection,
            Vector3 movement,
            EntityInstance entity) {

        EntityStateHandle state = entity.getEntityStateHandle();
        StatisticsHandle stats = entity.getStatisticsHandle();
        Vector2 vel = state.getHorizontalVelocity();
        float delta = internal.getDeltaTime();

        // Forward — flatten pitch to horizontal plane
        forward.x = cameraDirection.x;
        forward.y = 0f;
        forward.z = cameraDirection.z;
        forward.normalize();

        // Right — perpendicular to forward
        right.x = -forward.z;
        right.y = 0f;
        right.z = forward.x;

        // Target direction from input
        float targetX = forward.x * input.z + right.x * input.x;
        float targetZ = forward.z * input.z + right.z * input.x;

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