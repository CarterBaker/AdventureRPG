package application.bootstrap.physicspipeline.movementmanager;

import application.bootstrap.entitypipeline.entity.EntityInputHandle;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.entity.EntityState;
import application.bootstrap.entitypipeline.entity.EntityStateHandle;
import application.bootstrap.vehiclepipeline.vehiclemanager.VehicleRiderSystem;
import application.bootstrap.worldpipeline.util.WorldPositionStruct;
import application.bootstrap.worldpipeline.util.WorldPositionUtility;
import engine.root.ManagerPackage;
import engine.util.mathematics.vectors.Vector3;

public class MovementManager extends ManagerPackage {

    /*
     * Drives the full movement pipeline for any entity in a fixed order: liquid
     * contact and ladders, leaps, horizontal movement and the waves carrying
     * it, swimming, climbing or gravity, collision against vehicles and then
     * the world, correction, water state, position, boarding, ground offset,
     * facing, the hazards it met (a hard landing, a held breath) and the sound
     * of it all. A wall that held the stride back may start a ledge climb
     * through LedgeBranch, and a climb under way moves the entity along its
     * own path in place of every stroke, fall and wave until it stands on the
     * ledge. Vehicles are reached through
     * VehicleRiderSystem, which sweeps every move through their decks and
     * keeps whoever rides them.
     * fly() is the physics-free path for free cameras, and face() turns a body
     * without moving it. writeMovementState() is the one place an entity's
     * input decides how it moves on the ground, whoever drives it.
     */

    // Internal
    private VehicleRiderSystem vehicleRiderSystem;

    // Branches
    private MovementBranch movementBranch;
    private GravityBranch gravityBranch;
    private BlockCollisionBranch blockCollisionBranch;
    private SwimBranch swimBranch;
    private NaturalGroundOffsetBranch naturalGroundOffsetBranch;
    private FlightBranch flightBranch;
    private FacingBranch facingBranch;
    private LedgeBranch ledgeBranch;
    private HazardBranch hazardBranch;
    private MovementSoundBranch movementSoundBranch;

    // Cached Vectors
    private Vector3 movement;
    private Vector3 preCollisionSnapshot;

    // Internal \\

    @Override
    protected void create() {

        // Branches
        this.movementBranch = create(MovementBranch.class);
        this.gravityBranch = create(GravityBranch.class);
        this.blockCollisionBranch = create(BlockCollisionBranch.class);
        this.swimBranch = create(SwimBranch.class);
        this.naturalGroundOffsetBranch = create(NaturalGroundOffsetBranch.class);
        this.flightBranch = create(FlightBranch.class);
        this.facingBranch = create(FacingBranch.class);
        this.ledgeBranch = create(LedgeBranch.class);
        this.hazardBranch = create(HazardBranch.class);
        this.movementSoundBranch = create(MovementSoundBranch.class);

        // Cached Vectors
        this.movement = new Vector3();
        this.preCollisionSnapshot = new Vector3();
    }

    @Override
    protected void get() {
        this.vehicleRiderSystem = get(VehicleRiderSystem.class);
    }

    // Movement State \\

    // On the ground, the input decides the gait — a walk while asked or held back, a run while asked and not
    // attacking, else a stride, and standing when nothing moves it
    public void writeMovementState(EntityInstance entity, boolean slowed, boolean attacking) {

        EntityStateHandle state = entity.getEntityStateHandle();
        EntityInputHandle input = entity.getEntityInputHandle();

        if (!state.isGrounded())
            return;

        if (!input.hasHorizontalInput()) {
            state.setMovementState(EntityState.IDLE);
            return;
        }

        if (input.isWalk() || slowed)
            state.setMovementState(EntityState.WALKING);
        else if (input.isSprint() && !attacking)
            state.setMovementState(EntityState.RUNNING);
        else
            state.setMovementState(EntityState.MOVING);
    }

    // Movement \\

    public void move(EntityInstance entity) {

        Vector3 position = entity.getWorldPositionStruct().getPosition();
        EntityStateHandle state = entity.getEntityStateHandle();

        movement.set(0, 0, 0);

        if (state.isLedging()) {
            climbLedge(position, entity);
            return;
        }

        // 1. Liquid contact and ladders — a ladder held in the water lifts its climber out of it
        boolean touchingLiquid = swimBranch.refresh(entity);
        float jumpHeight = swimBranch.resolveJumpHeight(entity);
        boolean climbing = vehicleRiderSystem.refreshClimb(entity, touchingLiquid);
        boolean inWater = touchingLiquid && !climbing;

        // 2. Water leaps — running entry, or jumping out from the surface
        if (inWater && swimBranch.isEntryLeap(entity))
            gravityBranch.jump(entity, swimBranch.getEntryLeapHeight(entity), EntityState.WATER_LEAPING);
        else if (inWater && swimBranch.isSurfaceLeap(entity))
            gravityBranch.jump(entity, jumpHeight, EntityState.WATER_JUMPING);

        boolean swimming = inWater && swimBranch.isSwimming() && !state.isJumping();
        float dragMultiplier = inWater ? swimBranch.getSpeedMultiplier(entity, swimming) : 1f;

        // 3. Horizontal — the entity's own stroke, then the water carrying it
        movementBranch.calculate(movement, entity, dragMultiplier, swimming);

        if (inWater)
            swimBranch.carry(movement, entity, swimming);

        // 4. Vertical
        if (swimming)
            swimBranch.calculate(movement, entity);
        else if (climbing)
            vehicleRiderSystem.climb(movement, entity);
        else
            gravityBranch.calculate(movement, entity, jumpHeight);

        // 5. Snapshot before collision
        preCollisionSnapshot.set(movement.x, movement.y, movement.z);

        // 6. Collision — vehicle decks in their own frames, then the block grid, the only authority on solid/air
        boolean onDeck = vehicleRiderSystem.collide(movement, entity);
        blockCollisionBranch.calculate(position, movement, entity);

        // 7. Post-collision
        boolean climbedOut = inWater && swimBranch.attemptClimbOut(preCollisionSnapshot, movement, entity, swimming);

        if (!climbedOut && !swimming && onDeck)
            gravityBranch.land(entity);
        else if (!climbedOut && !swimming && !climbing)
            gravityBranch.postCollision(preCollisionSnapshot, movement, entity);
        else if (!climbedOut && swimming)
            swimBranch.postCollision(preCollisionSnapshot, movement, entity);

        // 7b. Ledges — a wall that held the stride back on land or in the air may be climbed
        if (!inWater && !swimming && !climbing && !onDeck)
            ledgeBranch.tryStart(position, preCollisionSnapshot, movement, entity);

        // 8. Water movement state — wading, treading, and water jump variants
        if (inWater && !climbedOut)
            swimBranch.resolveMovementState(entity, swimming);

        // 9. Apply, chunk update, and world wrap
        applyMovement(entity);

        // 10. Boarding — whoever stands on a deck or holds its ladder rides that vehicle
        vehicleRiderSystem.record(entity, swimming);

        // 11. Cosmetic ground offset — reads the now-final flat position only
        naturalGroundOffsetBranch.update(entity);

        // 12. Facing — turns the body toward the heading just travelled
        facingBranch.update(entity);

        // 13. Hazards — a hard landing and a held breath, read before the sound settles what was heard
        hazardBranch.update(entity, inWater, swimming, climbing, touchingLiquid && swimBranch.isEyeSubmerged(entity));

        // 14. Sound — steps, strokes, jumps, landings and splashes from what it just did
        movementSoundBranch.update(entity, inWater, swimming, climbing, onDeck);
    }

    // A ledge climb under way — its own path through the block grid, then everything that follows a move
    private void climbLedge(Vector3 position, EntityInstance entity) {

        ledgeBranch.advance(movement, entity);
        preCollisionSnapshot.set(movement.x, movement.y, movement.z);

        blockCollisionBranch.calculate(position, movement, entity);
        ledgeBranch.settle(preCollisionSnapshot, movement, entity);

        applyMovement(entity);
        naturalGroundOffsetBranch.update(entity);
        facingBranch.update(entity);
        hazardBranch.update(entity, false, false, true, false);
        movementSoundBranch.update(entity, false, false, true, false);
    }

    public void fly(EntityInstance entity) {

        movement.set(0, 0, 0);

        // 1. Free flight — no liquid, gravity, or collision
        flightBranch.calculate(movement, entity);

        // 2. Apply, chunk update, and world wrap
        applyMovement(entity);
    }

    public void face(EntityInstance entity) {
        facingBranch.update(entity);
    }

    // The free flight an entity's input asks for this frame, written out rather than applied, for a camera that
    // flies on someone else's keys
    public void resolveFlight(EntityInstance entity, Vector3 out) {
        out.set(0f, 0f, 0f);
        flightBranch.calculate(out, entity);
    }

    // Apply \\

    private void applyMovement(EntityInstance entity) {

        WorldPositionStruct worldPosition = entity.getWorldPositionStruct();

        // Speed
        recordSpeed(entity);

        // Apply
        worldPosition.getPosition().add(movement);

        // Chunk update and world wrap
        WorldPositionUtility.settleChunk(entity.getWorldHandle(), worldPosition);
    }

    private void recordSpeed(EntityInstance entity) {

        float delta = internal.getDeltaTime();

        if (delta <= 0f)
            return;

        float horizontal = (float) Math.sqrt(movement.x * movement.x + movement.z * movement.z);

        entity.getEntityStateHandle().setSpeed(horizontal / delta, movement.y / delta);
    }
}
