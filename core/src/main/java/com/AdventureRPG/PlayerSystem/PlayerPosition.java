package com.AdventureRPG.PlayerSystem;

import com.AdventureRPG.GameManager;
import com.AdventureRPG.InputSystem.Movement;
import com.AdventureRPG.Util.Vector2Int;
import com.AdventureRPG.Util.Vector3Int;
import com.AdventureRPG.WorldSystem.WorldSystem;
import com.badlogic.gdx.math.Vector3;

public class PlayerPosition {

    // Game Manager
    private final WorldSystem worldSystem;
    private final PlayerCamera camera;
    private final Movement movement;

    // Settings
    private final int CHUNK_SIZE;

    // Position
    private Vector3 currentPosition;
    private Vector2Int chunkCoordinate;

    // Base \\

    public PlayerPosition(GameManager gameManager, PlayerSystem playerSystem) {

        // Game Manager
        this.worldSystem = gameManager.worldSystem;
        this.camera = playerSystem.camera;
        this.movement = new Movement(gameManager, playerSystem.stats);

        // Settings
        this.CHUNK_SIZE = gameManager.settings.CHUNK_SIZE;

        // Position
        this.currentPosition = new Vector3();
        this.chunkCoordinate = new Vector2Int();
    }

    // Movement \\

    public void move(Vector3Int input) {

        currentPosition = movement.calculate(currentPosition, input, camera.direction());

        updateChunkCoordinateFrom(currentPosition);

        worldSystem.wrapAroundChunk(currentPosition);
        worldSystem.wrapAroundWorld(chunkCoordinate);

        worldSystem.updatePosition(currentPosition, chunkCoordinate);
    }

    // Calculate new chunk from position per axis
    private void updateChunkCoordinateFrom(Vector3 position) {

        chunkCoordinate.x += calculateChunkCoordinateAxisFrom(position.x);
        chunkCoordinate.y += calculateChunkCoordinateAxisFrom(position.z);
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
