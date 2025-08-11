package com.AdventureRPG.PlayerSystem;

import com.AdventureRPG.GameManager;
import com.AdventureRPG.InputSystem.Movement;
import com.AdventureRPG.Util.Vector3Int;
import com.AdventureRPG.WorldSystem.WorldSystem;
import com.badlogic.gdx.math.Vector3;

public class PlayerPosition {

    // Player
    private final WorldSystem worldSystem;
    private final PlayerCamera camera;
    private final Movement movement;

    // Position
    private Vector3 currentPosition;
    private Vector3Int chunkCoordinate;

    // Settings
    private final int CHUNK_SIZE;

    // Base \\

    public PlayerPosition(GameManager gameManager, PlayerSystem playerSystem) {

        // Player
        this.worldSystem = gameManager.worldSystem;
        this.camera = playerSystem.camera;
        this.movement = new Movement(gameManager, playerSystem.stats);

        // Position
        this.currentPosition = new Vector3();
        this.chunkCoordinate = new Vector3Int();

        // Settings
        this.CHUNK_SIZE = gameManager.settings.CHUNK_SIZE;
    }

    // Movement \\

    public void Move(Vector3Int input) {

        currentPosition = movement.Calculate(currentPosition, input, camera.Direction());

        UpdateChunkCoordinateFrom(currentPosition);

        worldSystem.WrapAroundChunk(currentPosition);
        worldSystem.WrapAroundWorld(chunkCoordinate);

        worldSystem.UpdatePosition(currentPosition, chunkCoordinate);
    }

    // Calculate new chunk from position per axis
    private void UpdateChunkCoordinateFrom(Vector3 position) {

        chunkCoordinate.x += CalculateChunkCoordinateAxisFrom(position.x);
        chunkCoordinate.y += CalculateChunkCoordinateAxisFrom(position.y);
        chunkCoordinate.z += CalculateChunkCoordinateAxisFrom(position.z);
    }

    // Use the calculated position this frame to calculate the new chunks position
    private int CalculateChunkCoordinateAxisFrom(float axis) {

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
