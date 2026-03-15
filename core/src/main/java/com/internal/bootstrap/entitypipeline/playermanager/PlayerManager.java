package com.internal.bootstrap.entitypipeline.playermanager;

import com.internal.bootstrap.entitypipeline.entity.EntityInstance;
import com.internal.bootstrap.entitypipeline.entity.EntityState;
import com.internal.bootstrap.entitypipeline.entity.EntityStateHandle;
import com.internal.bootstrap.entitypipeline.entitymanager.EntityManager;
import com.internal.bootstrap.entitypipeline.placementmanager.PlacementManager;
import com.internal.bootstrap.inputpipeline.inputsystem.InputSystem;
import com.internal.bootstrap.menupipeline.buttoneventsmanager.menus.InventoryBranch;
import com.internal.bootstrap.physicspipeline.movementmanager.MovementManager;
import com.internal.bootstrap.renderpipeline.camera.CameraInstance;
import com.internal.bootstrap.renderpipeline.cameramanager.CameraManager;
import com.internal.bootstrap.worldpipeline.blockmanager.BlockManager;
import com.internal.bootstrap.worldpipeline.chunk.ChunkData;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.chunkstreammanager.ChunkStreamManager;
import com.internal.bootstrap.worldpipeline.util.WorldPositionStruct;
import com.internal.bootstrap.worldpipeline.util.WorldPositionUtility;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.mathematics.vectors.Vector3;
import com.internal.core.util.mathematics.vectors.Vector3Int;

public class PlayerManager extends ManagerPackage {

    /*
     * Owns and drives the player entity. Handles spawn verification, camera
     * synchronization, movement state, inventory input, and placement input
     * each frame. Entity-agnostic input routing means AI can use the same
     * PlacementManager path as the player.
     */

    // Internal
    private CameraManager cameraManager;
    private InputSystem inputSystem;
    private MovementManager movementManager;
    private EntityManager entityManager;
    private BlockManager blockManager;
    private ChunkStreamManager chunkStreamManager;
    private InventoryBranch inventoryBranch;

    // Systems
    private InternalBufferSystem internalBufferSystem;
    private PlacementManager placementManager;

    // Player
    private EntityInstance player;
    private boolean verifyPlayerPosition;
    private Vector3 cameraPosition;
    private Vector3 cameraOffset;

    // Internal \\

    @Override
    protected void create() {

        // Systems
        this.internalBufferSystem = create(InternalBufferSystem.class);
        this.placementManager = create(PlacementManager.class);

        // Player
        this.verifyPlayerPosition = true;
        this.cameraPosition = new Vector3();
        this.cameraOffset = new Vector3();
    }

    @Override
    protected void get() {

        // Internal
        this.cameraManager = get(CameraManager.class);
        this.inputSystem = get(InputSystem.class);
        this.movementManager = get(MovementManager.class);
        this.entityManager = get(EntityManager.class);
        this.blockManager = get(BlockManager.class);
        this.chunkStreamManager = get(ChunkStreamManager.class);
        this.inventoryBranch = get(InventoryBranch.class);
    }

    @Override
    protected void awake() {

        // Player
        this.player = entityManager.spawnEntity(EngineSetting.DEFAULT_PLAYER_RACE);
        this.cameraOffset.set(
                player.getSize().x / 2,
                player.getEyeHeight(),
                player.getSize().z / 2);
    }

    @Override
    protected void update() {
        handleInventoryInput();
        calculatePlayerPosition();
    }

    // Inventory Input \\

    private void handleInventoryInput() {
        if (inputSystem.consumeInventoryJustPressed())
            inventoryBranch.toggleInventory(player);
    }

    // Player \\

    private void calculatePlayerPosition() {

        WorldPositionStruct worldPositionStruct = player.getWorldPositionStruct();

        if (verifyPlayerPosition) {
            verifyPlayerPosition = verifyPlayerPosition(worldPositionStruct);
            return;
        }

        Vector3Int input = inputSystem.getInput();
        CameraInstance camera = cameraManager.getMainCamera();
        Vector3 direction = camera.getDirection();

        writeMovementState(input);
        movementManager.move(input, direction, player);

        cameraPosition.set(worldPositionStruct.getPosition());
        cameraPosition.add(cameraOffset);
        camera.setPosition(cameraPosition);

        placementManager.update(
                player,
                cameraPosition,
                direction,
                inputSystem.isLeftClick(),
                inputSystem.isRightClick());

        internalBufferSystem.updatePlayerPosition(worldPositionStruct);
    }

    private void writeMovementState(Vector3Int input) {

        EntityStateHandle state = player.getEntityStateHandle();

        if (!state.isGrounded())
            return;

        boolean moving = input.x != 0 || input.z != 0;

        if (!moving) {
            state.setMovementState(EntityState.IDLE);
            return;
        }

        if (inputSystem.isWalkHeld())
            state.setMovementState(EntityState.WALKING);
        else if (inputSystem.isSprintHeld())
            state.setMovementState(EntityState.RUNNING);
        else
            state.setMovementState(EntityState.MOVING);
    }

    // Spawn Verification \\

    private boolean verifyPlayerPosition(WorldPositionStruct worldPositionStruct) {

        ChunkInstance activeChunkInstance = chunkStreamManager.getChunkInstance(
                worldPositionStruct.getChunkCoordinate());

        if (activeChunkInstance == null)
            return true;

        if (!activeChunkInstance.getChunkDataSyncContainer().hasData(ChunkData.GENERATION_DATA))
            return true;

        Vector3 position = worldPositionStruct.getPosition();
        int blockX = (int) position.x;
        int totalY = (int) position.y;
        int blockZ = (int) position.z;

        int safeY = WorldPositionUtility.findSafeSpawnHeight(
                activeChunkInstance, blockManager, blockX, totalY, blockZ);

        if (safeY == -1)
            return true;

        position.x = blockX;
        position.y = safeY;
        position.z = blockZ;

        return false;
    }

    // Accessible \\

    public EntityInstance getPlayer() {
        return player;
    }

    public WorldPositionStruct getPlayerPosition() {
        return player.getWorldPositionStruct();
    }
}