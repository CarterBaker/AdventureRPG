package com.AdventureRPG.PlayerSystem;

import com.AdventureRPG.Core.ManagerFrame;
import com.AdventureRPG.InputSystem.Movement;
import com.AdventureRPG.Util.GlobalConstant;
import com.AdventureRPG.Util.Vector2Int;
import com.AdventureRPG.Util.Vector3Int;
import com.AdventureRPG.WorldManager.WorldManager;
import com.badlogic.gdx.math.Vector3;

public class PositionManager extends ManagerFrame {

    // Root
    private Statistics statistics; // TODO: This can be removed when movement is refactored
    private PlayerCamera playerCamera;
    private Movement movement;
    private WorldManager worldManager;

    // Settings
    private int CHUNK_SIZE;

    // Position
    private Vector3 currentPosition;
    private Vector2Int chunkCoordinate;

    // Base \\

    public PositionManager(Statistics statistcs) {
        this.statistics = statistcs;
    }

    public void create() {

        // Root
        this.movement = (Movement) register(new Movement(statistics));

        // Settings
        this.CHUNK_SIZE = GlobalConstant.CHUNK_SIZE;

        // Position
        this.currentPosition = new Vector3();
        this.chunkCoordinate = new Vector2Int();
    }

    @Override
    public void init() {

        // Root
        this.playerCamera = localManager.get(PlayerCamera.class);
        this.worldManager = rootManager.get(WorldManager.class);
    }

    // Movement \\

    public void move(Vector3Int input) {

        currentPosition = movement.calculate(currentPosition, input, playerCamera.direction());

        updateChunkCoordinateFrom(currentPosition);

        worldManager.wrapAroundChunk(currentPosition);
        worldManager.wrapAroundWorld(chunkCoordinate);

        worldManager.updatePosition(currentPosition, chunkCoordinate);
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

    public Vector3 currentPosition() {
        return currentPosition;
    }

    public Vector2Int chunkCoordinate() {
        return chunkCoordinate;
    }
}
