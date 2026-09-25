package application.bootstrap.physicspipeline.movementmanager;

import application.bootstrap.entitypipeline.behavior.BehaviorHandle;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.entity.EntityState;
import application.bootstrap.entitypipeline.entity.EntityStateHandle;
import application.bootstrap.entitypipeline.util.EntityInputHandle;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3;

public class GravityBranch extends BranchPackage {

    /*
     * Applies gravity and jump force along all three axes based on the world
     * gravity direction each frame, writing displacement into the shared
     * movement vector passed by MovementManager. The jump height arrives
     * already resolved by SwimBranch, so water depth nerfs it without this
     * branch knowing about liquid; jump() is the single place a jump impulse
     * is applied, shared by grounded jumps and every water leap, and the jump
     * state it starts with is kept until the entity begins to fall. Landing
     * settles an airborne entity to IDLE, while an entity already on the
     * ground keeps the grounded state it was given. A grounded entity that
     * is not held up by the ground only turns FALLING once it drops faster
     * than GROUNDED_FALL_SPEED, so stepping down a block keeps its stride.
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