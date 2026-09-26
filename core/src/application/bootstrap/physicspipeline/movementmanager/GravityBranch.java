package application.bootstrap.physicspipeline.movementmanager;

import application.bootstrap.entitypipeline.behavior.BehaviorHandle;
import application.bootstrap.entitypipeline.entity.EntityInputHandle;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.entity.EntityState;
import application.bootstrap.entitypipeline.entity.EntityStateHandle;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3;

public class GravityBranch extends BranchPackage {

    /*
     * Applies gravity and jump force along the world's gravity direction.
     * jump() is the single place a jump impulse starts, with the height already
     * adjusted for water by SwimBranch. Landing settles to IDLE, and a grounded
     * entity only turns FALLING past GROUNDED_FALL_SPEED so stepping down keeps
     * its stride.
     */

    // Settings
    private float gravityForce;
    private float jumpScale;
    private float jumpHoldFraction;
    private float groundedFallSpeed;

    // Internal \\

    @Override
    protected void create() {

        // Settings
        this.gravityForce = EngineSetting.GRAVITY_FORCE;
        this.jumpScale = EngineSetting.JUMP_SCALE;
        this.jumpHoldFraction = EngineSetting.JUMP_HOLD_FRACTION;
        this.groundedFallSpeed = EngineSetting.GROUNDED_FALL_SPEED;
    }

    // Gravity \\

    void calculate(Vector3 movement, EntityInstance entity, float jumpHeight) {

        EntityStateHandle state = entity.getEntityStateHandle();
        BehaviorHandle behavior = entity.getBehaviorHandle();
        WorldHandle world = entity.getWorldHandle();
        EntityInputHandle input = entity.getEntityInputHandle();
        Vector3 gravVel = state.getGravityVelocity();
        Vector3 gravDir = world.getGravityDirection();
        float delta = internal.getDeltaTime();
        float gravMult = world.getGravityMultiplier();
        int verticalInput = input.getVertical();
        float gravLen = calculateGravityLength(gravDir);
        float jumpImpulse = calculateJumpImpulse(world, jumpHeight);

        // Jump initiation — instant velocity set opposite to gravity direction
        if (verticalInput == 1 && state.isGrounded())
            jump(entity, jumpHeight, EntityState.JUMPING);

        // Hold force — fraction of impulse applied opposite to gravity while held
        // within cap
        if (verticalInput == 1 && !state.isGrounded()) {
            float elapsed = (internal.getTime() - state.getJumpStartTime()) / EngineSetting.MILLIS_PER_SECOND_FLOAT;
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

        // State — negative dot = moving against gravity (keeps its jump state), positive = FALLING
        if (!state.isGrounded()) {
            float dot = gravVel.x * gravDir.x
                    + gravVel.y * gravDir.y
                    + gravVel.z * gravDir.z;
            if (dot >= 0f)
                state.setMovementState(EntityState.FALLING);
            else if (!state.isJumping())
                state.setMovementState(EntityState.JUMPING);
        }

        // Write displacement into shared movement vector
        movement.x += gravVel.x * delta;
        movement.y += gravVel.y * delta;
        movement.z += gravVel.z * delta;
    }

    void postCollision(Vector3 pre, Vector3 post, EntityInstance entity) {

        EntityStateHandle state = entity.getEntityStateHandle();
        Vector3 gravDir = entity.getWorldHandle().getGravityDirection();
        Vector3 gravVel = state.getGravityVelocity();

        boolean blocked = false;
        boolean movingWithGravity = false;

        if (gravDir.x != 0f
                && Math.abs(pre.x) > EngineSetting.GRAVITY_BLOCK_EPSILON
                && Math.abs(post.x) < EngineSetting.GRAVITY_BLOCK_EPSILON) {
            blocked = true;
            movingWithGravity = gravVel.x * gravDir.x > 0f;
            gravVel.x = 0f;
        }

        if (gravDir.y != 0f
                && Math.abs(pre.y) > EngineSetting.GRAVITY_BLOCK_EPSILON
                && Math.abs(post.y) < EngineSetting.GRAVITY_BLOCK_EPSILON) {
            blocked = true;
            movingWithGravity = gravVel.y * gravDir.y > 0f;
            gravVel.y = 0f;
        }

        if (gravDir.z != 0f
                && Math.abs(pre.z) > EngineSetting.GRAVITY_BLOCK_EPSILON
                && Math.abs(post.z) < EngineSetting.GRAVITY_BLOCK_EPSILON) {
            blocked = true;
            movingWithGravity = gravVel.z * gravDir.z > 0f;
            gravVel.z = 0f;
        }

        if (!blocked) {
            resolveUnsupported(state, gravVel, gravDir);
            return;
        }

        if (!movingWithGravity)
            state.setMovementState(EntityState.FALLING);
        else if (!state.isGrounded())
            state.setMovementState(EntityState.IDLE);
    }

    private void resolveUnsupported(EntityStateHandle state, Vector3 gravVel, Vector3 gravDir) {

        if (!state.isGrounded())
            return;

        float fallSpeed = (gravVel.x * gravDir.x + gravVel.y * gravDir.y + gravVel.z * gravDir.z)
                / calculateGravityLength(gravDir);

        if (fallSpeed > groundedFallSpeed)
            state.setMovementState(EntityState.FALLING);
    }

    // Jump \\

    void jump(EntityInstance entity, float jumpHeight, EntityState jumpState) {

        EntityStateHandle state = entity.getEntityStateHandle();
        WorldHandle world = entity.getWorldHandle();
        Vector3 gravVel = state.getGravityVelocity();
        Vector3 gravDir = world.getGravityDirection();
        float gravLen = calculateGravityLength(gravDir);
        float jumpImpulse = calculateJumpImpulse(world, jumpHeight);

        gravVel.x = (-gravDir.x / gravLen) * jumpImpulse;
        gravVel.y = (-gravDir.y / gravLen) * jumpImpulse;
        gravVel.z = (-gravDir.z / gravLen) * jumpImpulse;

        state.setJumpStartTime(internal.getTime());
        state.setMovementState(jumpState);
    }

    private float calculateJumpImpulse(WorldHandle world, float jumpHeight) {
        return (float) Math.sqrt(2.0 * gravityForce * world.getGravityMultiplier() * jumpHeight) * jumpScale;
    }

    private float calculateGravityLength(Vector3 gravDir) {

        float gravLen = (float) Math.sqrt(
                gravDir.x * gravDir.x + gravDir.y * gravDir.y + gravDir.z * gravDir.z);

        return gravLen == 0f ? 1f : gravLen;
    }
}