package application.bootstrap.physicspipeline.movementmanager;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.entity.EntityState;
import application.bootstrap.entitypipeline.entity.EntityStateHandle;
import application.bootstrap.worldpipeline.util.WorldPositionStruct;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.vectors.Vector3;

public class MovementManager extends ManagerPackage {

    /*
     * Drives the full movement pipeline for any entity each frame in a fixed
     * order: liquid contact, water leaps, horizontal movement, swimming or
     * gravity, collision, post-collision correction, the water movement
     * state, position application, and the cosmetic ground offset. SwimBranch
     * resolves water depth first and decides whether the entity wades (gravity
     * owns Y with a depth-nerfed jump) or swims (SwimBranch owns Y); any leap
     * hands Y back to gravity until it falls again. fly() is the physics-free
     * counterpart used by free cameras, and both paths share applyMovement().
     */

    // Internal
    private MovementBranch movementBranch;
    private GravityBranch gravityBranch;
    private BlockCollisionBranch blockCollisionBranch;
    private SwimBranch swimBranch;
    private NaturalGroundOffsetBranch naturalGroundOffsetBranch;
    private FlightBranch flightBranch;

    // Cached Vectors
    private Vector3 movement;
    private Vector3 preCollisionSnapshot;

    // Settings
    private int chunkSize;

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

        // Cached Vectors
        this.movement = new Vector3();
        this.preCollisionSnapshot = new Vector3();

        // Settings
        this.chunkSize = EngineSetting.CHUNK_SIZE;
    }

    // Movement \\

    public void move(EntityInstance entity) {

        Vector3 position = entity.getWorldPositionStruct().getPosition();
        EntityStateHandle state = entity.getEntityStateHandle();

        movement.set(0, 0, 0);

        // 1. Liquid contact
        boolean touchingLiquid = swimBranch.refresh(entity);
        float jumpHeight = swimBranch.resolveJumpHeight(entity);

        // 2. Water leaps — running entry, or jumping out from the surface
        if (swimBranch.isEntryLeap(entity))
            gravityBranch.jump(entity, swimBranch.getEntryLeapHeight(entity), EntityState.WATER_LEAPING);
        else if (swimBranch.isSurfaceLeap(entity))
            gravityBranch.jump(entity, jumpHeight, EntityState.WATER_JUMPING);

        boolean swimming = swimBranch.isSwimming() && !state.isJumping();
        float dragMultiplier = touchingLiquid ? swimBranch.getSpeedMultiplier(entity, swimming) : 1f;

        // 3. Horizontal
        movementBranch.calculate(movement, entity, dragMultiplier, swimming);

        // 4. Vertical
        if (swimming)
            swimBranch.calculate(movement, entity);
        else
            gravityBranch.calculate(movement, entity, jumpHeight);

        // 5. Snapshot before collision
        preCollisionSnapshot.set(movement.x, movement.y, movement.z);

        // 6. Collision — flat, jitter-free, the only authority on solid/air
        blockCollisionBranch.calculate(position, movement, entity);

        // 7. Post-collision
        boolean climbedOut = swimBranch.attemptClimbOut(preCollisionSnapshot, movement, entity, swimming);

        if (!climbedOut && !swimming)
            gravityBranch.postCollision(preCollisionSnapshot, movement, entity);

        // 8. Water movement state — wading, treading, and water jump variants
        if (!climbedOut)
            swimBranch.resolveMovementState(entity, swimming);

        // 9. Apply, chunk update, and world wrap
        applyMovement(entity);

        // 10. Cosmetic ground offset — reads the now-final flat position only
        naturalGroundOffsetBranch.update(entity);
    }

    public void fly(EntityInstance entity) {

        movement.set(0, 0, 0);

        // 1. Free flight — no liquid, gravity, or collision
        flightBranch.calculate(movement, entity);

        // 2. Apply, chunk update, and world wrap
        applyMovement(entity);
    }

    // Apply \\

    private void applyMovement(EntityInstance entity) {

        WorldPositionStruct worldPosition = entity.getWorldPositionStruct();
        Vector3 position = worldPosition.getPosition();
        long chunkCoordinate = worldPosition.getChunkCoordinate();
        int chunkCoordinateX = Coordinate2Long.unpackX(chunkCoordinate);
        int chunkCoordinateY = Coordinate2Long.unpackY(chunkCoordinate);

        // Apply
        position.x += movement.x;
        position.y += movement.y;
        position.z += movement.z;

        // Chunk update
        chunkCoordinate = updateChunkCoordinateFrom(position, chunkCoordinateX, chunkCoordinateY);

        // World wrap
        WorldWrapUtility.wrapAroundChunk(position);
        chunkCoordinate = WorldWrapUtility.wrapAroundWorld(entity.getWorldHandle(), chunkCoordinate);

        worldPosition.setPosition(position);
        worldPosition.setChunkCoordinate(chunkCoordinate);
    }

    // Chunk \\

    private long updateChunkCoordinateFrom(
            Vector3 position,
            int chunkCoordinateX,
            int chunkCoordinateY) {

        chunkCoordinateX += calculateChunkCoordinateAxisFrom(position.x);
        chunkCoordinateY += calculateChunkCoordinateAxisFrom(position.z);

        return Coordinate2Long.pack(chunkCoordinateX, chunkCoordinateY);
    }

    private int calculateChunkCoordinateAxisFrom(float axis) {

        float axisInput = axis;
        int newChunkAxis = 0;

        while (axisInput < 0) {
            axisInput += chunkSize;
            newChunkAxis -= 1;
        }

        while (axisInput >= chunkSize) {
            axisInput -= chunkSize;
            newChunkAxis += 1;
        }

        return newChunkAxis;
    }
}