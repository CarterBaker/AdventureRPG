package com.internal.bootstrap.entitypipeline.movementmanager;

import com.internal.bootstrap.entitypipeline.entity.EntityHandle;
import com.internal.bootstrap.entitypipeline.entityManager.StatisticsInstance;
import com.internal.bootstrap.worldpipeline.util.WorldPositionStruct;
import com.internal.bootstrap.worldpipeline.util.WorldWrapUtility;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.mathematics.Extras.Coordinate2Long;
import com.internal.core.util.mathematics.vectors.Vector3;
import com.internal.core.util.mathematics.vectors.Vector3Int;

public class MovementManager extends ManagerPackage {

    // Internal
    private MovementCalculationSystem movementCalculationSystem;

    // Settings
    private int CHUNK_SIZE;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.movementCalculationSystem = create(MovementCalculationSystem.class);

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

        StatisticsInstance statistics = entityHandle.getStatisticsInstance();

        position = movementCalculationSystem.calculate(
                input,
                direction,
                position,
                statistics);

        chunkCoordinate = updateChunkCoordinateFrom(
                position,
                chunkCoordinateX,
                chunkCoordinateY);

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
