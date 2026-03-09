package com.internal.bootstrap.physicspipeline.movementmanager.movement;

import com.internal.bootstrap.entitypipeline.behavior.BehaviorHandle;
import com.internal.bootstrap.entitypipeline.entity.EntityHandle;
import com.internal.bootstrap.entitypipeline.entity.EntityState;
import com.internal.bootstrap.entitypipeline.entity.EntityStateHandle;
import com.internal.bootstrap.entitypipeline.statistics.StatisticsStruct;
import com.internal.bootstrap.worldpipeline.world.WorldHandle;
import com.internal.core.engine.BranchPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.mathematics.vectors.Vector3;

public class GravityBranch extends BranchPackage {

    // Settings
    private float GRAVITY_FORCE;
    private float JUMP_SCALE;
    private float JUMP_HOLD_FRACTION;

    // Cached displacement — returned each frame, no per-frame allocation
    private Vector3 displacement;

    // Internal \\

    @Override
    protected void create() {
        this.GRAVITY_FORCE = EngineSetting.GRAVITY_FORCE;
        this.JUMP_SCALE = EngineSetting.JUMP_SCALE;
        this.JUMP_HOLD_FRACTION = EngineSetting.JUMP_HOLD_FRACTION;
        this.displacement = new Vector3();
    }

    // Gravity \\

    /**
     * Applies gravity and jump force along all three axes based on world gravity
     * direction.
     * Jump direction is always opposite to gravity direction.
     * State is derived from dot product of gravity velocity against gravity
     * direction —
     * negative dot = moving against gravity = JUMPING, positive = FALLING.
     * Returns cached displacement vector (velocity * delta). Do not store the
     * reference.
     */
    public Vector3 calculate(int verticalInput, EntityHandle entity) {

        EntityStateHandle state = entity.getEntityStateHandle();
        BehaviorHandle behavior = entity.getBehaviorHandle();
        WorldHandle world = entity.getWorldHandle();
        StatisticsStruct stats = entity.getStatisticsInstance();
        Vector3 gravVel = state.getGravityVelocity();
        Vector3 gravDir = world.getGravityDirection();
        float delta = internal.getDeltaTime();
        float gravMult = world.getGravityMultiplier();

        // Normalize gravity direction for jump axis calculation
        float gravLen = (float) Math.sqrt(
                gravDir.x * gravDir.x + gravDir.y * gravDir.y + gravDir.z * gravDir.z);
        if (gravLen == 0f)
            gravLen = 1f;

        // Derive jump impulse from real jump height — sqrt(2 * g * h) * scale
        float jumpImpulse = (float) Math.sqrt(
                2.0 * GRAVITY_FORCE * gravMult * stats.jumpHeight) * JUMP_SCALE;

        // Jump initiation — instant velocity set opposite to gravity direction
        if (verticalInput == 1 && state.isGrounded()) {
            gravVel.x = (-gravDir.x / gravLen) * jumpImpulse;
            gravVel.y = (-gravDir.y / gravLen) * jumpImpulse;
            gravVel.z = (-gravDir.z / gravLen) * jumpImpulse;
            state.setJumpStartTime(internal.getTime());
            state.setMovementState(EntityState.JUMPING);
        }

        // Hold force — fraction of impulse applied opposite to gravity while held
        // within cap
        if (verticalInput == 1 && !state.isGrounded()) {
            float elapsed = (internal.getTime() - state.getJumpStartTime()) / 1000f;
            if (elapsed < behavior.getJumpDuration()) {
                float holdForce = (jumpImpulse * JUMP_HOLD_FRACTION) * delta;
                gravVel.x += (-gravDir.x / gravLen) * holdForce;
                gravVel.y += (-gravDir.y / gravLen) * holdForce;
                gravVel.z += (-gravDir.z / gravLen) * holdForce;
            }
        }

        // Gravity — always acts along all non-zero gravity axes
        gravVel.x += gravDir.x * gravMult * GRAVITY_FORCE * delta;
        gravVel.y += gravDir.y * gravMult * GRAVITY_FORCE * delta;
        gravVel.z += gravDir.z * gravMult * GRAVITY_FORCE * delta;

        // State — dot product tells us direction relative to gravity
        // negative = moving against gravity (JUMPING), positive = moving with gravity
        // (FALLING)
        if (!state.isGrounded()) {
            float dot = gravVel.x * gravDir.x
                    + gravVel.y * gravDir.y
                    + gravVel.z * gravDir.z;
            state.setMovementState(dot < 0f ? EntityState.JUMPING : EntityState.FALLING);
        }

        // Write displacement to cached vector — caller must not store the reference
        displacement.x = gravVel.x * delta;
        displacement.y = gravVel.y * delta;
        displacement.z = gravVel.z * delta;

        return displacement;
    }

    /**
     * Call after collision with pre-collision movement snapshot and post-collision
     * movement.
     * For each non-zero gravity axis, checks if that component was blocked.
     * Uses gravity velocity direction to determine landing vs ceiling hit.
     */
    public void postCollision(Vector3 pre, Vector3 post, EntityHandle entity) {

        EntityStateHandle state = entity.getEntityStateHandle();
        Vector3 gravDir = entity.getWorldHandle().getGravityDirection();
        Vector3 gravVel = state.getGravityVelocity();

        boolean blocked = false;
        boolean movingWithGravity = false;

        // Check each non-zero gravity axis
        if (gravDir.x != 0f && Math.abs(pre.x) > 0.0001f && Math.abs(post.x) < 0.0001f) {
            blocked = true;
            // dot of gravity velocity against gravity direction on this axis
            movingWithGravity = (gravVel.x * gravDir.x > 0f);
            gravVel.x = 0f;
        }

        if (gravDir.y != 0f && Math.abs(pre.y) > 0.0001f && Math.abs(post.y) < 0.0001f) {
            blocked = true;
            movingWithGravity = (gravVel.y * gravDir.y > 0f);
            gravVel.y = 0f;
        }

        if (gravDir.z != 0f && Math.abs(pre.z) > 0.0001f && Math.abs(post.z) < 0.0001f) {
            blocked = true;
            movingWithGravity = (gravVel.z * gravDir.z > 0f);
            gravVel.z = 0f;
        }

        if (blocked) {
            if (movingWithGravity)
                state.setMovementState(EntityState.IDLE); // landed
            else
                state.setMovementState(EntityState.FALLING); // hit ceiling
        }
    }
}