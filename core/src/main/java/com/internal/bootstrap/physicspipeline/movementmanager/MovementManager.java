package com.internal.bootstrap.physicspipeline.movementmanager;

import com.internal.bootstrap.entitypipeline.entity.EntityHandle;
import com.internal.bootstrap.physicspipeline.movementmanager.movement.BlockCollisionBranch;
import com.internal.bootstrap.physicspipeline.movementmanager.movement.GravityBranch;
import com.internal.bootstrap.physicspipeline.movementmanager.movement.MovementBranch;
import com.internal.bootstrap.worldpipeline.util.WorldPositionStruct;
import com.internal.bootstrap.worldpipeline.util.WorldWrapUtility;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.mathematics.Extras.Coordinate2Long;
import com.internal.core.util.mathematics.vectors.Vector3;
import com.internal.core.util.mathematics.vectors.Vector3Int;

public class MovementManager extends ManagerPackage {

    // Internal
    private MovementBranch movementBranch;
    private GravityBranch gravityBranch;
    private BlockCollisionBranch blockCollisionBranch;

    // Cached vectors — no per-frame allocation
    private Vector3 movement;
    private Vector3 preCollisionSnapshot;

    // Settings
    private int CHUNK_SIZE;

    // Internal \\

    @Override
    protected void create() {
        this.movementBranch = create(MovementBranch.class);
        this.gravityBranch = create(GravityBranch.class);
        this.blockCollisionBranch = create(BlockCollisionBranch.class);
        this.movement = new Vector3();
        this.preCollisionSnapshot = new Vector3();
        this.CHUNK_SIZE = EngineSetting.CHUNK_SIZE;
    }

    // Movement \\

    /**
     * Moves an entity based on a normalized Vector3Int input.
     * Does not know or care if input came from player or AI.
     */
    public void move(Vector3Int input, Vector3 direction, EntityHandle entity) {

        WorldPositionStruct worldPosition = entity.getWorldPositionStruct();
        Vector3 position = worldPosition.getPosition();
        long chunkCoordinate = worldPosition.getChunkCoordinate();
        int chunkCoordinateX = Coordinate2Long.unpackX(chunkCoordinate);
        int chunkCoordinateY = Coordinate2Long.unpackY(chunkCoordinate);

        entity.update();

        // Reset movement each frame — prevents stale data carrying over between
        // branches
        movement.set(0, 0, 0);

        // 1. Horizontal — x, z only
        movementBranch.calculate(input, direction, movement, entity);

        // 2. Gravity — adds to all three axes, direction and magnitude from WorldHandle
        Vector3 gravDisp = gravityBranch.calculate(input.y, entity);
        movement.x += gravDisp.x;
        movement.y += gravDisp.y;
        movement.z += gravDisp.z;

        // 3. Snapshot before collision
        preCollisionSnapshot.set(movement.x, movement.y, movement.z);

        // 4. Collision
        blockCollisionBranch.calculate(position, movement, entity);

        // 5. Post-collision
        gravityBranch.postCollision(preCollisionSnapshot, movement, entity);

        // 6. Apply
        position.x += movement.x;
        position.y += movement.y;
        position.z += movement.z;

        // 7. Chunk update
        chunkCoordinate = updateChunkCoordinateFrom(position, chunkCoordinateX, chunkCoordinateY);

        // 8. World wrap
        WorldWrapUtility.wrapAroundChunk(position);
        chunkCoordinate = WorldWrapUtility.wrapAroundWorld(entity.getWorldHandle(), chunkCoordinate);

        worldPosition.setPosition(position);
        worldPosition.setChunkCoordinate(chunkCoordinate);
    }

    // Chunk \\

    private long updateChunkCoordinateFrom(Vector3 position, int chunkCoordinateX, int chunkCoordinateY) {
        chunkCoordinateX += calculateChunkCoordinateAxisFrom(position.x);
        chunkCoordinateY += calculateChunkCoordinateAxisFrom(position.z);
        return Coordinate2Long.pack(chunkCoordinateX, chunkCoordinateY);
    }

    private int calculateChunkCoordinateAxisFrom(float axis) {
        float axisInput = axis;
        int newChunkAxis = 0;
        while (axisInput < 0) {
            axisInput += CHUNK_SIZE;
            newChunkAxis -= 1;
        }
        while (axisInput >= CHUNK_SIZE) {
            axisInput -= CHUNK_SIZE;
            newChunkAxis += 1;
        }
        return newChunkAxis;
    }
}