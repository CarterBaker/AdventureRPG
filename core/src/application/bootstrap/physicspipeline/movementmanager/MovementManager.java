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
     * the water's own surface height — without it, an entity that can't touch
     * bottom has no way back onto dry land, however shallow the water looks
     * from the bank.
     */

    // Internal
    private MovementBranch movementBranch;
    private GravityBranch gravityBranch;
    private BlockCollisionBranch blockCollisionBranch;
    private SwimBranch swimBranch;

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

        // 1. Water contact — decides for the whole frame whether the entity is
        // wading (feet on the bottom, gravity still owns the Y axis) or fully
        // swimming (SwimBranch owns the Y axis), and how much drag the touched
        // liquid applies either way.
        boolean touchingLiquid = swimBranch.refresh(entity);
        boolean swimming = touchingLiquid && swimBranch.isSwimming(entity);
        boolean wading = touchingLiquid && !swimming;
        float dragMultiplier = touchingLiquid ? swimBranch.getSpeedMultiplier() : 1f;

        // 2. Horizontal — x, z only
        movementBranch.calculate(movement, entity, dragMultiplier, swimming);

        // 3. Vertical — swimming owns this axis once genuinely submerged;
        // gravity owns it otherwise, wading or not. A wading entity just rides
        // gravity/collision down onto whatever floor sits beneath it, same as
        // dry ground, only with a diminished jump.
        if (swimming)
            swimBranch.calculate(movement, entity);
        else
            gravityBranch.calculate(movement, entity, wading);

        // 4. Snapshot before collision
        preCollisionSnapshot.set(movement.x, movement.y, movement.z);

        // 5. Collision
        blockCollisionBranch.calculate(position, movement, entity);

        // 6. Post-collision — only relevant to whichever branch drove the
        // Y axis this frame. While swimming, a blocked horizontal move is
        // instead handed to attemptClimbOut() in case it was blocked by a
        // bank low enough to pull the entity out onto.
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