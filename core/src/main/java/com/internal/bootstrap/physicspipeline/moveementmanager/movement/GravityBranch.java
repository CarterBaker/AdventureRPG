package com.internal.bootstrap.physicspipeline.moveementmanager.movement;

import com.internal.bootstrap.entitypipeline.behavior.BehaviorHandle;
import com.internal.bootstrap.entitypipeline.entity.EntityHandle;
import com.internal.bootstrap.entitypipeline.entity.EntityState;
import com.internal.bootstrap.entitypipeline.entity.EntityStateHandle;
import com.internal.bootstrap.entitypipeline.statistics.StatisticsStruct;
import com.internal.bootstrap.worldpipeline.world.WorldHandle;
import com.internal.core.engine.BranchPackage;
import com.internal.core.engine.settings.EngineSetting;

public class GravityBranch extends BranchPackage {

    // Settings
    private float GRAVITY_FORCE;
    private float JUMP_SCALE;
    private float JUMP_HOLD_FRACTION;

    // Internal \\

    @Override
    protected void create() {
        this.GRAVITY_FORCE = EngineSetting.GRAVITY_FORCE;
        this.JUMP_SCALE = EngineSetting.JUMP_SCALE;
        this.JUMP_HOLD_FRACTION = EngineSetting.JUMP_HOLD_FRACTION;
    }

    // Gravity \\

    /**
     * verticalInput == 1 → jump held
     * verticalInput == 0 → no vertical input
     * verticalInput == -1 → reserved (crouch / swim down later)
     *
     * jumpHeight is a real-world value in meters.
     * Impulse is derived: sqrt(2 * g * h) — the exact velocity needed to reach that
     * height.
     * JUMP_SCALE shifts it into comfortable game feel without changing the stat
     * meaning.
     */
    public float calculate(int verticalInput, EntityHandle entity) {

        EntityStateHandle state = entity.getEntityStateHandle();
        BehaviorHandle behavior = entity.getBehaviorHandle();
        WorldHandle world = entity.getWorldHandle();
        StatisticsStruct stats = entity.getStatisticsInstance();
        float delta = internal.getDeltaTime();

        // Derive impulse from real jump height — sqrt(2 * g * h) * game feel scale
        float jumpImpulse = (float) Math.sqrt(2.0 * GRAVITY_FORCE * stats.jumpHeight) * JUMP_SCALE;

        // Jump initiation — set velocity to derived impulse instantly
        if (verticalInput == 1 && state.isGrounded()) {
            state.setVerticalVelocity(jumpImpulse);
            state.setJumpStartTime(internal.getTime());
            state.setMovementState(EntityState.JUMPING);
        }

        // Hold — fraction of impulse added per second while held within behavior cap
        if (verticalInput == 1 && !state.isGrounded()) {
            float elapsed = (internal.getTime() - state.getJumpStartTime()) / 1000f;
            if (elapsed < behavior.getJumpDuration()) {
                state.setVerticalVelocity(
                        state.getVerticalVelocity() + (jumpImpulse * JUMP_HOLD_FRACTION) * delta);
            }
        }

        // Gravity — always acts every frame
        float gravityAccel = world.getGravityDirection().y
                * world.getGravityMultiplier()
                * GRAVITY_FORCE;

        state.setVerticalVelocity(state.getVerticalVelocity() + gravityAccel * delta);

        // State reflects velocity — only while airborne
        if (!state.isGrounded()) {
            if (state.getVerticalVelocity() > 0f)
                state.setMovementState(EntityState.JUMPING);
            else
                state.setMovementState(EntityState.FALLING);
        }

        return state.getVerticalVelocity() * delta;
    }

    /**
     * Call after collision.
     * Blocked vertical movement resolves landing and ceiling hits.
     */
    public void postCollision(float preCollisionY, float postCollisionY, EntityHandle entity) {

        EntityStateHandle state = entity.getEntityStateHandle();

        if (preCollisionY != 0f && postCollisionY == 0f) {
            state.setVerticalVelocity(0f);

            if (preCollisionY < 0f)
                state.setMovementState(EntityState.IDLE);
            else
                state.setMovementState(EntityState.FALLING);
        }
    }
}