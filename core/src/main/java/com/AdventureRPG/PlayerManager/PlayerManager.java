package com.AdventureRPG.PlayerManager;

import com.AdventureRPG.Core.Bootstrap.ManagerFrame;
import com.AdventureRPG.Core.PhysicsPipeline.InputSystem.InputSystem;
import com.AdventureRPG.Core.PhysicsPipeline.MovementManager.MovementManager;
import com.AdventureRPG.Core.RenderPipeline.CameraSystem.CameraInstance;
import com.AdventureRPG.Core.RenderPipeline.CameraSystem.CameraSystem;
import com.AdventureRPG.Core.Util.Vector2Int;
import com.AdventureRPG.WorldManager.WorldManager;
import com.badlogic.gdx.math.Vector3;

// TODO: This needs to be abstracted to generic NPC like class
public class PlayerManager extends ManagerFrame {

    // Player
    private StatisticsInstance statisticsInstance;
    private InputSystem inputSystem;
    private CameraSystem cameraSystem;
    private MovementManager movementManager;
    private WorldManager worldManager;

    // Camera
    private CameraInstance cameraInstance;

    // Position
    private Vector3 currentPosition; // TODO: These are very special they should be built into the engine itself
    private Vector2Int currentChunk;

    // Base \\

    @Override
    protected void create() {

        // Player
        this.statisticsInstance = new StatisticsInstance();

        // Position
        this.currentPosition = new Vector3();
        this.currentChunk = new Vector2Int();
    }

    @Override
    protected void init() {

        // Player
        this.inputSystem = engineManager.get(InputSystem.class);
        this.cameraSystem = engineManager.get(CameraSystem.class);
        this.movementManager = engineManager.get(MovementManager.class);
        this.worldManager = engineManager.get(WorldManager.class);

        // Camera
        this.cameraInstance = cameraSystem.mainCamera();
    }

    @Override
    protected void fixedUpdate() {

        movementManager.move(
                statisticsInstance,
                inputSystem.getInput(),
                cameraSystem.mainCamera().direction(),
                currentPosition,
                currentChunk);

        cameraSystem.rotateCamera(cameraInstance, inputSystem.getRotation());
        cameraSystem.moveCamera(cameraInstance, currentPosition);

        worldManager.updatePosition(currentPosition, currentChunk);
    }

    // Accessible \\

    public Vector3 getCurrentPosition() {
        return currentPosition;
    }

    public Vector2Int getCurrentChunk() {
        return currentChunk;
    }
}
