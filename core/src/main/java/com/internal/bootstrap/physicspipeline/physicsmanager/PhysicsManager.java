package com.internal.bootstrap.physicspipeline.physicsmanager;

import com.internal.bootstrap.entitypipeline.entityManager.EntityHandle;
import com.internal.bootstrap.entitypipeline.entityManager.StatisticsStruct;
import com.internal.bootstrap.physicspipeline.physicsmanager.physics.BlockCollisionBranch;
import com.internal.bootstrap.physicspipeline.physicsmanager.physics.MovementBranch;
import com.internal.bootstrap.worldpipeline.util.WorldPositionStruct;
import com.internal.bootstrap.worldpipeline.util.WorldWrapUtility;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.mathematics.Extras.Coordinate2Long;
import com.internal.core.util.mathematics.vectors.Vector3;
import com.internal.core.util.mathematics.vectors.Vector3Int;

public class PhysicsManager extends ManagerPackage {
    // Internal
    private MovementBranch movementBranch;
    private BlockCollisionBranch blockCollisionBranch;
    private Vector3 movement;

    // Settings
    private int CHUNK_SIZE;

    // Internal \\
    @Override
    protected void create() {
        // Internal
        this.movementBranch = create(MovementBranch.class);
        this.blockCollisionBranch = create(BlockCollisionBranch.class);
        this.movement = new Vector3();

        // Settings
        this.CHUNK_SIZE = EngineSetting.CHUNK_SIZE;
    }

    // Movement \\
    public void move(
            Vector3Int input,
            Vector3 direction,
            EntityHandle entityHandle) {

        WorldPositionStruct worldPosition = entityHandle.getWorldPositionStruct();
        Vector3 position = worldPosition.getPosition();
        long chunkCoordinate = worldPosition.getChunkCoordinate();
        int chunkCoordinateX = Coordinate2Long.unpackX(chunkCoordinate);
        int chunkCoordinateY = Coordinate2Long.unpackY(chunkCoordinate);
        StatisticsStruct statistics = entityHandle.getStatisticsInstance();

        entityHandle.update();

        movementBranch.calculate(
                input,
                direction,
                movement,
                statistics);

        blockCollisionBranch.calculate(
                input,
                position,
                movement,
                entityHandle);

        // Apply movement to position
        position.x += movement.x;
        position.y += movement.y;
        position.z += movement.z;

        // Update chunk coordinate based on new position
        chunkCoordinate = updateChunkCoordinateFrom(
                position,
                chunkCoordinateX,
                chunkCoordinateY);

        // Apply world wrapping
        WorldWrapUtility.wrapAroundChunk(position);
        chunkCoordinate = WorldWrapUtility.wrapAroundWorld(entityHandle.getWorldHandle(), chunkCoordinate);

        worldPosition.setPosition(position);
        worldPosition.setChunkCoordinate(chunkCoordinate);
    }

    // Calculate new chunk from position per axis
    private long updateChunkCoordinateFrom(
            Vector3 position,
            int chunkCoordinateX,
            int chunkCoordinateY) {
        chunkCoordinateX += calculateChunkCoordinateAxisFrom(position.x);
        chunkCoordinateY += calculateChunkCoordinateAxisFrom(position.z);
        return Coordinate2Long.pack(chunkCoordinateX, chunkCoordinateY);
    }

    // Use the calculated position this frame to calculate the new chunks position
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