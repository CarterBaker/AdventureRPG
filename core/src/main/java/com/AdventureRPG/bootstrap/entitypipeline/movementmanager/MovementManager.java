package com.AdventureRPG.bootstrap.entitypipeline.movementmanager;

import com.AdventureRPG.bootstrap.entitypipeline.entity.EntityHandle;
import com.AdventureRPG.bootstrap.entitypipeline.entityManager.StatisticsInstance;
import com.AdventureRPG.bootstrap.worldpipeline.util.WorldPositionStruct;
import com.AdventureRPG.bootstrap.worldpipeline.util.WorldWrapUtility;
import com.AdventureRPG.bootstrap.worldpipeline.worldstreammanager.WorldHandle;
import com.AdventureRPG.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import com.AdventureRPG.core.engine.ManagerPackage;
import com.AdventureRPG.core.engine.settings.EngineSetting;
import com.AdventureRPG.core.util.mathematics.Extras.Coordinate2Int;
import com.AdventureRPG.core.util.mathematics.vectors.Vector3;
import com.AdventureRPG.core.util.mathematics.vectors.Vector3Int;

public class MovementManager extends ManagerPackage {

    // Internal
    private MovementCalculationSystem movementCalculationSystem;
    private WorldStreamManager worldStreamManager;

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

    @Override
    protected void get() {

        // Internal
        this.worldStreamManager = get(WorldStreamManager.class);
    }

    // Movement \\

    public void move(
            Vector3Int input,
            Vector3 direction,
            EntityHandle entityHandle) {

        WorldPositionStruct worldPosition = entityHandle.getWorldPositionStruct();
        Vector3 position = worldPosition.getPosition();

        long chunkCoordinate = worldPosition.getChunkCoordinate();
        int chunkCoordinateX = Coordinate2Int.unpackX(chunkCoordinate);
        int chunkCoordinateY = Coordinate2Int.unpackY(chunkCoordinate);

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

        return Coordinate2Int.pack(chunkCoordinateX, chunkCoordinateY);
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
