package application.bootstrap.physicspipeline.movementmanager;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.entity.EntityState;
import application.bootstrap.entitypipeline.entity.EntityStateHandle;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.vectors.Vector3;

public class LedgeBranch extends BranchPackage {

    /*
     * Climbs ledges a stair step cannot. An entity on the ground that walks
     * into a ledge above a stair step but no higher than the clamber height
     * clambers up it with its hands; one in the air, jumping or falling, that
     * meets a wall whose ledge its arms reach above its head grabs the edge
     * and mantles up, lower ledges met in the air clambered as from the
     * ground. tryStart() finds the ledge through BlockCollisionBranch, which
     * sweeps the rise and the step onto it clear before the climb begins, and
     * starts it in its state with its fall forgotten, so a grab never lands
     * as a fall, rising a hair past the ledge so the step onto it never
     * catches its edge. advance() lays the climb's movement for the frame: the rise
     * eased over its span of the duration, then the step onto the ledge over
     * its own, each written as what is left of its target so a step a corner
     * held back is made up once the body clears it. settle() records what the
     * collision let through, drops the entity into a fall when its rise
     * stalls against something set in its way, and stands it on the ledge
     * once the duration has run. The animation tree plays the state's clip
     * for exactly that duration.
     */

    // Internal
    private BlockCollisionBranch blockCollisionBranch;

    // Settings
    private float stepHeight;
    private float clamberHeight;
    private float armReach;
    private float forwardBlocks;
    private float blockedShare;
    private float stallShare;
    private float clearance;

    // Scratch
    private Vector3 probePosition;

    // Internal \\

    @Override
    protected void create() {

        // Settings
        this.stepHeight = EngineSetting.STEP_UP_HEIGHT_BLOCKS;
        this.clamberHeight = EngineSetting.LEDGE_CLAMBER_HEIGHT_BLOCKS;
        this.armReach = EngineSetting.LEDGE_ARM_REACH_BLOCKS;
        this.forwardBlocks = EngineSetting.LEDGE_FORWARD_BLOCKS;
        this.blockedShare = EngineSetting.LEDGE_BLOCKED_SHARE;
        this.stallShare = EngineSetting.LEDGE_STALL_SHARE;
        this.clearance = EngineSetting.LEDGE_CLEARANCE_BLOCKS;

        // Scratch
        this.probePosition = new Vector3();
    }

    @Override
    protected void get() {
        this.blockCollisionBranch = get(BlockCollisionBranch.class);
    }

    // Start \\

    // A climb begins when a wall held the entity's stride back and a ledge stands within its reach, true once begun
    boolean tryStart(Vector3 position, Vector3 intended, Vector3 moved, EntityInstance entity) {

        EntityStateHandle state = entity.getEntityStateHandle();

        if (!entity.getEntityInputHandle().hasHorizontalInput())
            return false;

        float intendedDistance = horizontalDistance(intended.x, intended.z);

        if (intendedDistance <= 0f
                || horizontalDistance(moved.x, moved.z) > intendedDistance * blockedShare)
            return false;

        boolean grounded = state.isGrounded();
        float maxRise = grounded ? clamberHeight : entity.getSize().y + armReach;
        float forwardX = intended.x / intendedDistance * forwardBlocks;
        float forwardZ = intended.z / intendedDistance * forwardBlocks;

        probePosition.set(position).add(moved);

        float rise = blockCollisionBranch.resolveLedge(probePosition, entity, forwardX, forwardZ, maxRise);

        if (rise <= stepHeight)
            return false;

        EntityState ledgeState = rise <= clamberHeight ? EntityState.CLAMBERING : EntityState.MANTLING;

        state.startLedge(ledgeState, rise + clearance, forwardX, forwardZ);
        state.getGravityVelocity().set(0f, 0f, 0f);
        state.getHorizontalVelocity().set(0f, 0f);
        state.setHeard(false, 0f, state.isHeardInLiquid());

        return true;
    }

    // Climb \\

    // The climb's movement this frame — what is left of the rise and the step onto the ledge their spans have reached
    void advance(Vector3 movement, EntityInstance entity) {

        EntityStateHandle state = entity.getEntityStateHandle();
        boolean mantling = state.getMovementState() == EntityState.MANTLING;
        float time = state.getLedgeTime() + internal.getDeltaTime();
        float share = Math.min(time / resolveDuration(mantling), 1f);

        state.setLedgeTime(time);

        float rise = mantling
                ? ease(share, EngineSetting.LEDGE_MANTLE_RISE_START, EngineSetting.LEDGE_MANTLE_RISE_END)
                : ease(share, EngineSetting.LEDGE_CLAMBER_RISE_START, EngineSetting.LEDGE_CLAMBER_RISE_END);
        float forward = mantling
                ? ease(share, EngineSetting.LEDGE_MANTLE_FORWARD_START, EngineSetting.LEDGE_MANTLE_FORWARD_END)
                : ease(share, EngineSetting.LEDGE_CLAMBER_FORWARD_START, EngineSetting.LEDGE_CLAMBER_FORWARD_END);
        float stepShare = forward - state.getLedgeAdvanced();

        movement.set(
                state.getLedgeForwardX() * stepShare,
                state.getLedgeRise() * (rise - state.getLedgeRisen()),
                state.getLedgeForwardZ() * stepShare);
    }

    // What the collision let through recorded, a stalled rise dropped into a fall, a finished climb stood on its ledge
    void settle(Vector3 intended, Vector3 moved, EntityInstance entity) {

        EntityStateHandle state = entity.getEntityStateHandle();
        float rise = state.getLedgeRise();
        float forward = horizontalDistance(state.getLedgeForwardX(), state.getLedgeForwardZ());

        if (intended.y > 0f && moved.y < intended.y * stallShare) {
            state.setMovementState(EntityState.FALLING);
            return;
        }

        float risen = state.getLedgeRisen() + moved.y / rise;
        float advanced = state.getLedgeAdvanced() + horizontalDistance(moved.x, moved.z) / forward;

        state.setLedgeProgress(risen, advanced);

        boolean mantling = state.getMovementState() == EntityState.MANTLING;

        if (state.getLedgeTime() >= resolveDuration(mantling))
            state.setMovementState(EntityState.IDLE);
    }

    // Utility \\

    private float resolveDuration(boolean mantling) {
        return mantling ? EngineSetting.LEDGE_MANTLE_DURATION : EngineSetting.LEDGE_CLAMBER_DURATION;
    }

    // The share of a span the climb has made, eased in and out across it
    private float ease(float share, float start, float end) {

        float t = Math.min(Math.max((share - start) / (end - start), 0f), 1f);

        return t * t * (3f - 2f * t);
    }

    private float horizontalDistance(float x, float z) {
        return (float) Math.sqrt(x * x + z * z);
    }
}
