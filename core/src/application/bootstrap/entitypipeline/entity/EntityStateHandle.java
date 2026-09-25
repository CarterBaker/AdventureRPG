package application.bootstrap.entitypipeline.entity;

import engine.root.HandlePackage;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector3;

public class EntityStateHandle extends HandlePackage {

    /*
     * Per-entity runtime movement state. Holds current movement state, gravity
     * and horizontal velocity accumulators, jump start time, whether the entity
     * was touching liquid last frame, and the smoothed cosmetic vertical ground
     * offset NaturalGroundOffsetBranch derives from whichever natural block
     * currently sits beneath this entity's feet. horizontalSpeed and
     * verticalSpeed are the displacement MovementManager actually applied
     * last frame, after collision, in blocks per second. bodyYaw is the
     * smoothed way the body faces, in degrees, and bodyYawRate how fast it
     * is turning; lookPitch and lookYaw are where the entity looks relative
     * to that body — all written by FacingBranch. No manager owns this — it
     * lives directly on EntityInstance.
     */

    // State
    private EntityState movementState;

    // Velocity
    private Vector3 gravityVelocity;
    private Vector2 horizontalVelocity;
    private long jumpStartTime;
    private float horizontalSpeed;
    private float verticalSpeed;

    // Facing
    private float bodyYaw;
    private float bodyYawRate;
    private float lookPitch;
    private float lookYaw;

    // Liquid
    private boolean inLiquid;

    // Ground Offset — cosmetic only, never fed back into physics position
    private float groundOffset;

    // Internal \\

    @Override
    protected void create() {

        // State
        this.movementState = EntityState.IDLE;

        // Velocity
        this.gravityVelocity = new Vector3();
        this.horizontalVelocity = new Vector2();
        this.jumpStartTime = 0L;
        this.horizontalSpeed = 0f;
        this.verticalSpeed = 0f;

        // Facing
        this.bodyYaw = 0f;
        this.bodyYawRate = 0f;
        this.lookPitch = 0f;
        this.lookYaw = 0f;

        // Liquid
        this.inLiquid = false;

        // Ground Offset
        this.groundOffset = 0f;
    }

    // Accessible \\

    public EntityState getMovementState() {
        return movementState;
    }

    public void setMovementState(EntityState movementState) {
        this.movementState = movementState;
    }

    public Vector3 getGravityVelocity() {
        return gravityVelocity;
    }

    public Vector2 getHorizontalVelocity() {
        return horizontalVelocity;
    }

    public long getJumpStartTime() {
        return jumpStartTime;
    }

    public void setJumpStartTime(long jumpStartTime) {
        this.jumpStartTime = jumpStartTime;
    }

    public float getHorizontalSpeed() {
        return horizontalSpeed;
    }

    public float getVerticalSpeed() {
        return verticalSpeed;
    }

    public void setSpeed(float horizontalSpeed, float verticalSpeed) {
        this.horizontalSpeed = horizontalSpeed;
        this.verticalSpeed = verticalSpeed;
    }

    public float getBodyYaw() {
        return bodyYaw;
    }

    public float getBodyYawRate() {
        return bodyYawRate;
    }

    public void setBodyYaw(float bodyYaw, float bodyYawRate) {
        this.bodyYaw = bodyYaw;
        this.bodyYawRate = bodyYawRate;
    }

    public float getLookPitch() {
        return lookPitch;
    }

    public float getLookYaw() {
        return lookYaw;
    }

    public void setLook(float lookPitch, float lookYaw) {
        this.lookPitch = lookPitch;
        this.lookYaw = lookYaw;
    }

    public boolean isInLiquid() {
        return inLiquid;
    }

    public void setInLiquid(boolean inLiquid) {
        this.inLiquid = inLiquid;
    }

    public float getGroundOffset() {
        return groundOffset;
    }

    public void setGroundOffset(float groundOffset) {
        this.groundOffset = groundOffset;
    }

    // Utility \\

    public boolean isGrounded() {
        return !isJumping()
                && !isSwimming()
                && movementState != EntityState.FALLING;
    }

    public boolean isSwimming() {
        return switch (movementState) {
            case SWIMMING, TREADING, UNDERWATER_SWIMMING, UNDERWATER_TREADING, DIVING, SURFACING -> true;
            default -> false;
        };
    }

    public boolean isJumping() {
        return movementState == EntityState.JUMPING
                || movementState == EntityState.WATER_JUMPING
                || movementState == EntityState.WATER_LEAPING;
    }
}