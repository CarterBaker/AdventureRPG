package com.internal.bootstrap.physicspipeline.movementmanager.movement;

import com.internal.bootstrap.entitypipeline.entity.EntityHandle;
import com.internal.bootstrap.entitypipeline.entity.EntityState;
import com.internal.bootstrap.entitypipeline.entity.EntityStateHandle;
import com.internal.bootstrap.entitypipeline.statistics.StatisticsStruct;
import com.internal.core.engine.BranchPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.mathematics.vectors.Vector2;
import com.internal.core.util.mathematics.vectors.Vector3;
import com.internal.core.util.mathematics.vectors.Vector3Int;

public class MovementBranch extends BranchPackage {

    // Settings
    private float MOVEMENT_ACCELERATION;
    private float MOVEMENT_SCALE;

    // Internal
    private Vector3 forward;
    private Vector3 right;

    // Internal \\

    @Override
    protected void create() {
        this.MOVEMENT_ACCELERATION = EngineSetting.MOVEMENT_ACCELERATION;
        this.MOVEMENT_SCALE = EngineSetting.MOVEMENT_SCALE;
        this.forward = new Vector3();
        this.right = new Vector3();
    }

    // Movement \\

    /**
     * Horizontal movement only — Y is owned by GravityBranch.
     * Real-world speeds from StatisticsStruct are scaled by MOVEMENT_SCALE for game
     * feel.
     * Does not know or care if input came from player or AI.
     */
    public void calculate(
            Vector3Int input,
            Vector3 cameraDirection,
            Vector3 movement,
            EntityHandle entity) {

        EntityStateHandle state = entity.getEntityStateHandle();
        StatisticsStruct stats = entity.getStatisticsInstance();
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
        float targetX = 0f;
        float targetZ = 0f;

        targetX += forward.x * input.z;
        targetZ += forward.z * input.z;
        targetX += right.x * input.x;
        targetZ += right.z * input.x;

        // Normalize target direction
        float len = (float) Math.sqrt(targetX * targetX + targetZ * targetZ);
        if (len > 0f) {
            targetX /= len;
            targetZ /= len;
        }

        // Real-world speed scaled by MOVEMENT_SCALE for game feel
        float speed = selectSpeed(state.getMovementState(), stats) * MOVEMENT_SCALE;
        targetX *= speed;
        targetZ *= speed;

        // Lerp toward target — smooth acceleration ramp
        float lerpFactor = Math.min(1f, delta * MOVEMENT_ACCELERATION);
        vel.x += (targetX - vel.x) * lerpFactor;
        vel.y += (targetZ - vel.y) * lerpFactor;

        // Write to movement — Y untouched
        movement.x = vel.x * delta;
        movement.z = vel.y * delta;
    }

    // Speed \\

    private float selectSpeed(EntityState state, StatisticsStruct stats) {
        return switch (state) {
            case WALKING -> stats.walkSpeed;
            case MOVING -> stats.movementSpeed;
            case RUNNING -> stats.sprintSpeed;
            case JUMPING, FALLING -> stats.movementSpeed;
            default -> stats.movementSpeed;
        };
    }
}