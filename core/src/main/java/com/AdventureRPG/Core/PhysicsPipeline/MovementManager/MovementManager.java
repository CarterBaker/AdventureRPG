package com.AdventureRPG.Core.PhysicsPipeline.MovementManager;

import com.AdventureRPG.Core.Bootstrap.EngineSetting;
import com.AdventureRPG.Core.Bootstrap.ManagerFrame;
import com.AdventureRPG.Core.ScenePipeline.WorldEngineSystem.WorldEngineSystem;
import com.AdventureRPG.Core.Util.Methematics.Vectors.Vector2Int;
import com.AdventureRPG.Core.Util.Methematics.Vectors.Vector3Int;
import com.AdventureRPG.PlayerManager.StatisticsInstance;
import com.badlogic.gdx.math.Vector3;

public class MovementManager extends ManagerFrame {

    // Root
    private MovementCalculationSystem movementCalculationSystem;
    private WorldEngineSystem worldEngineSystem;

    // Settings
    private int CHUNK_SIZE;

    // Base \\

    @Override
    protected void create() {

        // Root
        this.movementCalculationSystem = (MovementCalculationSystem) register(new MovementCalculationSystem());

        // Settings
        this.CHUNK_SIZE = EngineSetting.CHUNK_SIZE;
    }

    @Override
    protected void init() {

        // Root
        this.worldEngineSystem = gameEngine.get(WorldEngineSystem.class);
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
