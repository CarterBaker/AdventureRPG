package com.AdventureRPG.playermanager;

import com.AdventureRPG.WorldPipeline.WorldPipeline;
import com.AdventureRPG.core.engine.ManagerPackage;
import com.AdventureRPG.core.physicspipeline.input.InputSystem;
import com.AdventureRPG.core.physicspipeline.movement.MovementManager;
import com.AdventureRPG.core.renderpipeline.camerasystem.CameraManager;
import com.AdventureRPG.core.util.Mathematics.Vectors.Vector2Int;
import com.badlogic.gdx.math.Vector3;

// TODO: This needs to be abstracted to generic NPC like class
public class PlayerManager extends ManagerPackage {

    // Player
    private StatisticsInstance statisticsInstance;
    private InputSystem inputSystem;
    private CameraManager cameraManager;
    private MovementManager movementManager;
    private WorldPipeline worldPipeline;

    // Position
    private Vector3 currentPosition; // TODO: These are very special they should be built into the engine itself
    private Vector2Int currentChunk;

    // Base \\

    @Override
    protected void create() {

        // Player
        this.statisticsInstance = create(StatisticsInstance.class);

        // Position
        this.currentPosition = new Vector3();
        this.currentChunk = new Vector2Int();
    }

    @Override
    protected void get() {

        // Player
        this.inputSystem = get(InputSystem.class);
        this.cameraManager = get(CameraManager.class);
        this.movementManager = get(MovementManager.class);
        this.worldPipeline = get(WorldPipeline.class);
    }

    @Override
    protected void update() {
        cameraManager.rotateCamera(inputSystem.getRotation());
    }

    @Override
    protected void fixedUpdate() {

        movementManager.move(
                statisticsInstance,
                inputSystem.getInput(),
                cameraManager.mainCamera().direction(),
                currentPosition,
                currentChunk);

        // TODO: Along with the camera system not sure I want ot be doing this
        cameraManager.moveCamera(currentPosition);

        worldPipeline.updatePosition(currentPosition, currentChunk);
    }

    // Accessible \\

    public Vector3 getCurrentPosition() {
        return currentPosition;
    }

    public Vector2Int getCurrentChunk() {
        return currentChunk;
    }
}
