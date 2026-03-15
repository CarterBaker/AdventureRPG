package com.internal.bootstrap.physicspipeline.movementmanager;

import com.internal.bootstrap.entitypipeline.behavior.BehaviorHandle;
import com.internal.bootstrap.entitypipeline.entity.EntityInstance;
import com.internal.bootstrap.entitypipeline.entity.EntityState;
import com.internal.bootstrap.entitypipeline.entity.EntityStateHandle;
import com.internal.bootstrap.entitypipeline.statistics.StatisticsHandle;
import com.internal.bootstrap.worldpipeline.world.WorldHandle;
import com.internal.core.engine.BranchPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.mathematics.vectors.Vector3;

public class GravityBranch extends BranchPackage {

    /*
     * Applies gravity and jump force along all three axes based on the world
     * gravity direction each frame. Writes displacement directly into the
     * shared movement vector passed by MovementManager.
     */

    // Settings
    private float gravityForce;
    private float jumpScale;
    private float jumpHoldFraction;

    // Internal \\

    @Override
    protected void create() {

        // Settings
        this.gravityForce = EngineSetting.GRAVITY_FORCE;
        this.jumpScale = EngineSetting.JUMP_SCALE;
        this.jumpHoldFraction = EngineSetting.JUMP_HOLD_FRACTION;
    }

    // Gravity \\

    /*
     * Applies gravity and jump force along all three axes based on world gravity
     * direction. Jump direction is always opposite to gravity direction.
     * State is derived from dot product of gravity velocity against gravity
     * direction — negative dot means moving against gravity (JUMPING), positive
     * means falling. Writes displacement directly into movement.
     */
    void calculate(int verticalInput, Vector3 movement, EntityInstance entity) {

        EntityStateHandle state = entity.getEntityStateHandle();
        BehaviorHandle behavior = entity.getBehaviorHandle();
        WorldHandle world = entity.getWorldHandle();
        StatisticsHandle stats = entity.getStatisticsHandle();
        Vector3 gravVel = state.getGravityVelocity();
        Vector3 gravDir = world.getGravityDirection();
        float delta = internal.getDeltaTime();
        float gravMult = world.getGravityMultiplier();

        float gravLen = (float) Math.sqrt(
                gravDir.x * gravDir.x + gravDir.y * gravDir.y + gravDir.z * gravDir.z);

        if (gravLen == 0f)
            gravLen = 1f;

        float jumpImpulse = (float) Math.sqrt(
                2.0 * gravityForce * gravMult * stats.getJumpHeight()) * jumpScale;

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
                float holdForce = (jumpImpulse * jumpHoldFraction) * delta;
                gravVel.x += (-gravDir.x / gravLen) * holdForce;
                gravVel.y += (-gravDir.y / gravLen) * holdForce;
                gravVel.z += (-gravDir.z / gravLen) * holdForce;
            }
        }

        // Gravity — always acts along all non-zero gravity axes
        gravVel.x += gravDir.x * gravMult * gravityForce * delta;
        gravVel.y += gravDir.y * gravMult * gravityForce * delta;
        gravVel.z += gravDir.z * gravMult * gravityForce * delta;

        // State — negative dot = moving against gravity (JUMPING), positive = FALLING
        if (!state.isGrounded()) {
            float dot = gravVel.x * gravDir.x
                    + gravVel.y * gravDir.y
                    + gravVel.z * gravDir.z;
            state.setMovementState(dot < 0f ? EntityState.JUMPING : EntityState.FALLING);
        }

        // Write displacement into shared movement vector
        movement.x += gravVel.x * delta;
        movement.y += gravVel.y * delta;
        movement.z += gravVel.z * delta;
    }

    /*
     * Call after collision with pre-collision movement snapshot and post-collision
     * movement. For each non-zero gravity axis, checks if that component was
     * blocked. Uses gravity velocity direction to determine landing vs ceiling hit.
     */
    void postCollision(Vector3 pre, Vector3 post, EntityInstance entity) {

        EntityStateHandle state = entity.getEntityStateHandle();
        Vector3 gravDir = entity.getWorldHandle().getGravityDirection();
        Vector3 gravVel = state.getGravityVelocity();

        boolean blocked = false;
        boolean movingWithGravity = false;

        if (gravDir.x != 0f && Math.abs(pre.x) > 0.0001f && Math.abs(post.x) < 0.0001f) {
            blocked = true;
            movingWithGravity = gravVel.x * gravDir.x > 0f;
            gravVel.x = 0f;
        }

        if (gravDir.y != 0f && Math.abs(pre.y) > 0.0001f && Math.abs(post.y) < 0.0001f) {
            blocked = true;
            movingWithGravity = gravVel.y * gravDir.y > 0f;
            gravVel.y = 0f;
        }

        if (gravDir.z != 0f && Math.abs(pre.z) > 0.0001f && Math.abs(post.z) < 0.0001f) {
            blocked = true;
            movingWithGravity = gravVel.z * gravDir.z > 0f;
            gravVel.z = 0f;
        }

        if (!blocked)
            return;

        if (movingWithGravity)
            state.setMovementState(EntityState.IDLE);
        else
            state.setMovementState(EntityState.FALLING);
    }
}