package com.AdventureRPG.bootstrap.entitypipeline.playermanager;

import com.AdventureRPG.bootstrap.entitypipeline.entity.EntityHandle;
import com.AdventureRPG.bootstrap.entitypipeline.entityManager.EntityManager;
import com.AdventureRPG.bootstrap.entitypipeline.movementmanager.MovementManager;
import com.AdventureRPG.bootstrap.inputpipeline.input.InputSystem;
import com.AdventureRPG.bootstrap.renderpipeline.camera.CameraInstance;
import com.AdventureRPG.bootstrap.renderpipeline.camerasystem.CameraManager;
import com.AdventureRPG.bootstrap.worldpipeline.util.WorldPositionStruct;
import com.AdventureRPG.core.engine.ManagerPackage;
import com.AdventureRPG.core.util.mathematics.vectors.Vector3;
import com.AdventureRPG.core.util.mathematics.vectors.Vector3Int;

public class PlayerManager extends ManagerPackage {

    // Internal
    private CameraManager cameraManager;
    private InputSystem inputSystem;
    private MovementManager movementmanager;
    private EntityManager entityManager;

    // Active Player
    private EntityHandle player;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.cameraManager = get(CameraManager.class);
        this.inputSystem = get(InputSystem.class);
        this.movementmanager = get(MovementManager.class);
        this.entityManager = get(EntityManager.class);
    }

    @Override
    protected void awake() {

        // Active Player
        this.player = entityManager.createEntity();
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

    // Accessible \\

    public EntityHandle getPlayer() {
        return player;
    }

    public WorldPositionStruct getPlayerPosition() {
        return player.getWorldPositionStruct();
    }
}
