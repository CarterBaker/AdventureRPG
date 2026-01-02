package com.AdventureRPG.core.physicspipeline.movement;

import com.AdventureRPG.core.engine.ManagerPackage;
import com.AdventureRPG.core.engine.settings.EngineSetting;
import com.AdventureRPG.core.scenepipeline.worldenginesystem.WorldEngineSystem;
import com.AdventureRPG.core.util.Mathematics.Vectors.Vector2Int;
import com.AdventureRPG.core.util.Mathematics.Vectors.Vector3Int;
import com.AdventureRPG.playermanager.StatisticsInstance;
import com.badlogic.gdx.math.Vector3;

public class MovementManager extends ManagerPackage {

    // Root
    private MovementCalculationSystem movementCalculationSystem;
    private WorldEngineSystem worldEngineSystem;

    // Settings
    private int CHUNK_SIZE;

    // Base \\

    @Override
    protected void create() {

        // Root
        this.movementCalculationSystem = create(MovementCalculationSystem.class);

        // Settings
        this.CHUNK_SIZE = EngineSetting.CHUNK_SIZE;
    }

    @Override
    protected void get() {

        // Root
        this.worldEngineSystem = get(WorldEngineSystem.class);
    }

    // Movement \\

    public void move(
            StatisticsInstance statisticsInstance,
            Vector3Int input,
            Vector3 direction,
            Vector3 currentPosition,
            Vector2Int currentChunk) {

        currentPosition = movementCalculationSystem.calculate(
                statisticsInstance,
                currentPosition,
                input,
                direction);

        updateChunkCoordinateFrom(
                currentPosition,
                currentChunk);

        worldEngineSystem.wrapAroundChunk(currentPosition);
        worldEngineSystem.wrapAroundWorld(currentChunk);
    }

    // Calculate new chunk from position per axis
    private void updateChunkCoordinateFrom(
            Vector3 position,
            Vector2Int currentChunk) {

        currentChunk.x += calculateChunkCoordinateAxisFrom(position.x);
        currentChunk.y += calculateChunkCoordinateAxisFrom(position.z);
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
