package com.AdventureRPG.bootstrap.entitypipeline.playermanager;

import com.AdventureRPG.bootstrap.entitypipeline.entityManager.EntityHandle;
import com.AdventureRPG.bootstrap.entitypipeline.entityManager.EntityManager;
import com.AdventureRPG.bootstrap.entitypipeline.movementmanager.MovementManager;
import com.AdventureRPG.bootstrap.inputpipeline.input.InputSystem;
import com.AdventureRPG.bootstrap.renderpipeline.camerasystem.CameraInstance;
import com.AdventureRPG.bootstrap.renderpipeline.camerasystem.CameraManager;
import com.AdventureRPG.bootstrap.worldpipeline.util.WorldPositionStruct;
import com.AdventureRPG.core.engine.ManagerPackage;
import com.AdventureRPG.core.util.mathematics.Extras.Coordinate2Int;
import com.AdventureRPG.core.util.mathematics.vectors.Vector3;
import com.AdventureRPG.core.util.mathematics.vectors.Vector3Int;

public class PlayerManager extends ManagerPackage {

    // Internal
    private CameraManager cameraManager;
    private InputSystem inputSystem;
    private MovementManager movementmanager;

    // Active Player
    private EntityHandle player;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.cameraManager = get(CameraManager.class);
        this.inputSystem = get(InputSystem.class);
        this.movementmanager = get(MovementManager.class);

        // Active Player
        this.player = get(EntityManager.class).createEntity();
    }

    @Override
    protected void update() {
        calculatePlayerPosition();
    }

    private void calculatePlayerPosition() {

        // Get required reference data
        Vector3Int input = inputSystem.getInput();
        CameraInstance camera = cameraManager.getMainCamera();
        Vector3 direction = camera.getDirection();

        movementmanager.move(input, direction, player);
    }
}
