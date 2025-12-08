package com.AdventureRPG.playermanager;

import com.AdventureRPG.core.kernel.ManagerFrame;
import com.AdventureRPG.core.physicspipeline.input.InputSystem;
import com.AdventureRPG.core.physicspipeline.movement.MovementManager;
import com.AdventureRPG.core.renderpipeline.camerasystem.CameraSystem;
import com.AdventureRPG.core.util.Methematics.Vectors.Vector2Int;
import com.AdventureRPG.worldmanager.WorldManager;
import com.badlogic.gdx.math.Vector3;

// TODO: This needs to be abstracted to generic NPC like class
public class PlayerManager extends ManagerFrame {

    // Player
    private StatisticsInstance statisticsInstance;
    private InputSystem inputSystem;
    private CameraSystem cameraSystem;
    private MovementManager movementManager;
    private WorldManager worldManager;

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
        this.inputSystem = gameEngine.get(InputSystem.class);
        this.cameraSystem = gameEngine.get(CameraSystem.class);
        this.movementManager = gameEngine.get(MovementManager.class);
        this.worldManager = gameEngine.get(WorldManager.class);
    }

    @Override
    protected void update() {
        cameraSystem.rotateCamera(inputSystem.getRotation());
    }

    @Override
    protected void fixedUpdate() {

        movementManager.move(
                statisticsInstance,
                inputSystem.getInput(),
                cameraSystem.mainCamera().direction(),
                currentPosition,
                currentChunk);

        // TODO: Along with the camera system not sure I want ot be doing this
        cameraSystem.moveCamera(currentPosition);

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
