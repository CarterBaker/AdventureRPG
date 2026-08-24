package application.bootstrap.physicspipeline.movementmanager;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.worldpipeline.util.WorldPositionStruct;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.vectors.Vector3;

public class MovementManager extends ManagerPackage {

    /*
     * Drives the full movement pipeline for any entity each frame. Coordinates
     * horizontal movement, swimming/wading/gravity, collision, post-collision
     * correction, position application, and chunk boundary updates in a fixed
     * order. Reads all input from the entity's InputHandle — never touches
     * InputSystem directly.
     *
     * SwimBranch.refresh() runs first and decides, for the rest of the frame,
     * whether the entity is touching liquid at all and — separately — whether
     * that liquid is deep enough to fully submerge it. Not deep enough to
     * submerge means wading: gravity keeps the Y axis (with a diminished jump,
     * see GravityBranch), horizontal drag still applies, and the movement
     * state is EntityState.WADING. Deep enough to submerge means swimming:
     * SwimBranch owns the Y axis instead, and attemptClimbOut() gets a chance
     * right after collision to pull the entity out onto any bank at or below
     * the water's own surface height.
     *
     * NaturalGroundOffsetBranch runs last, strictly after position and chunk
     * wrap are final. It never writes back into position — it only maintains
     * a smoothed cosmetic value that camera/eye code reads separately — so it
     * can never affect collision, gravity, or block composition.
     */

    // Internal
    private MovementBranch movementBranch;
    private GravityBranch gravityBranch;
    private BlockCollisionBranch blockCollisionBranch;
    private SwimBranch swimBranch;
    private NaturalGroundOffsetBranch naturalGroundOffsetBranch;

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

        // Cached Vectors
        this.movement = new Vector3();
        this.preCollisionSnapshot = new Vector3();

        // Settings
        this.chunkSize = EngineSetting.CHUNK_SIZE;
    }

    // Movement \\

    public void move(EntityInstance entity) {

        WorldPositionStruct worldPosition = entity.getWorldPositionStruct();
        Vector3 position = worldPosition.getPosition();
        long chunkCoordinate = worldPosition.getChunkCoordinate();
        int chunkCoordinateX = Coordinate2Long.unpackX(chunkCoordinate);
        int chunkCoordinateY = Coordinate2Long.unpackY(chunkCoordinate);

        movement.set(0, 0, 0);

        // 1. Water contact
        boolean touchingLiquid = swimBranch.refresh(entity);
        boolean swimming = touchingLiquid && swimBranch.isSwimming(entity);
        boolean wading = touchingLiquid && !swimming;
        float dragMultiplier = touchingLiquid ? swimBranch.getSpeedMultiplier() : 1f;

        // 2. Horizontal
        movementBranch.calculate(movement, entity, dragMultiplier, swimming);

        // 3. Vertical
        if (swimming)
            swimBranch.calculate(movement, entity);
        else
            gravityBranch.calculate(movement, entity, wading);

        // 4. Snapshot before collision
        preCollisionSnapshot.set(movement.x, movement.y, movement.z);

        // 5. Collision — flat, jitter-free, the only authority on solid/air
        blockCollisionBranch.calculate(position, movement, entity);

        // 6. Post-collision
        if (swimming)
            swimBranch.attemptClimbOut(preCollisionSnapshot, movement, entity);
        else
            gravityBranch.postCollision(preCollisionSnapshot, movement, entity, wading);

        // 7. Apply
        position.x += movement.x;
        position.y += movement.y;
        position.z += movement.z;

        // 8. Chunk update
        chunkCoordinate = updateChunkCoordinateFrom(position, chunkCoordinateX, chunkCoordinateY);

        // 9. World wrap
        WorldWrapUtility.wrapAroundChunk(position);
        chunkCoordinate = WorldWrapUtility.wrapAroundWorld(entity.getWorldHandle(), chunkCoordinate);

        worldPosition.setPosition(position);
        worldPosition.setChunkCoordinate(chunkCoordinate);

        // 10. Cosmetic ground offset — reads the now-final flat position only
        naturalGroundOffsetBranch.update(entity);
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