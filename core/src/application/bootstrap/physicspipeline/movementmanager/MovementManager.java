package application.bootstrap.physicspipeline.movementmanager;

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
     * the world, correction, water state, position, boarding, ground offset
     * and facing. Vehicles are reached through VehicleRiderSystem, which
     * sweeps every move through their decks and keeps whoever rides them.
     * fly() is the physics-free path for free cameras, and face() turns a body
     * without moving it.
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

        // Cached Vectors
        this.movement = new Vector3();
        this.preCollisionSnapshot = new Vector3();
    }

    @Override
    protected void get() {
        this.vehicleRiderSystem = get(VehicleRiderSystem.class);
    }

    // Movement \\

    public void move(EntityInstance entity) {

        Vector3 position = entity.getWorldPositionStruct().getPosition();
        EntityStateHandle state = entity.getEntityStateHandle();

        movement.set(0, 0, 0);

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
