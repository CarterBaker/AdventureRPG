package com.internal.bootstrap.entitypipeline.playermanager;

import com.internal.bootstrap.entitypipeline.entity.EntityData;
import com.internal.bootstrap.entitypipeline.entity.EntityHandle;
import com.internal.bootstrap.entitypipeline.entity.EntityState;
import com.internal.bootstrap.entitypipeline.entity.EntityStateHandle;
import com.internal.bootstrap.entitypipeline.entitymanager.EntityManager;
import com.internal.bootstrap.inputpipeline.inputsystem.InputSystem;
import com.internal.bootstrap.physicspipeline.moveementmanager.MovementManager;
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

    // Internal
    private CameraManager cameraManager;
    private InputSystem inputSystem;
    private MovementManager movementManager;
    private EntityManager entityManager;
    private BlockManager blockManager;
    private ChunkStreamManager chunkStreamManager;

    private InternalBufferSystem internalBufferSystem;
    private BlockPlacementSystem blockPlacementSystem;

    // Active Player
    private EntityData entityData;
    private EntityHandle player;
    private boolean verifyPlayerPosition;
    private Vector3 cameraPosition;
    private Vector3 cameraOffset;

    // Internal \\

    @Override
    protected void create() {
        this.internalBufferSystem = create(InternalBufferSystem.class);
        this.blockPlacementSystem = create(BlockPlacementSystem.class);
        this.verifyPlayerPosition = true;
        this.cameraPosition = new Vector3();
        this.cameraOffset = new Vector3();
    }

    @Override
    protected void get() {
        this.cameraManager = get(CameraManager.class);
        this.inputSystem = get(InputSystem.class);
        this.movementManager = get(MovementManager.class);
        this.entityManager = get(EntityManager.class);
        this.blockManager = get(BlockManager.class);
        this.chunkStreamManager = get(ChunkStreamManager.class);
    }

    @Override
    protected void awake() {
        this.entityData = entityManager.getTemplateDataFromTemplateName(EngineSetting.DEFAULT_PLAYER_RACE);
        this.player = entityManager.createEntity(entityData);
        this.cameraOffset.set(
                player.getSize().x / 2,
                player.getEyeHeight(),
                player.getSize().z / 2);
    }

    @Override
    protected void update() {
        calculatePlayerPosition();
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

        // Write movement state intent from input modifiers
        // Vertical state (JUMPING/FALLING) is owned by GravityBranch — not touched here
        writeMovementState(input);

        // Hand input vector and entity to MovementManager — no physics logic here
        // AI systems will call movementManager.move() directly with their own input
        movementManager.move(input, direction, player);

        // Camera follow
        cameraPosition.set(worldPositionStruct.getPosition());
        cameraPosition.add(cameraOffset);
        camera.setPosition(cameraPosition);

        // Block interaction
        blockPlacementSystem.update(
                player,
                cameraPosition,
                direction,
                player.getStatisticsInstance(),
                inputSystem.isLeftClick(),
                inputSystem.isRightClick());

        internalBufferSystem.updatePlayerPosition(worldPositionStruct);
    }

    /**
     * Translates input modifier keys into movement state.
     * Only sets horizontal state — vertical state is owned by GravityBranch.
     * This is the only place InputSystem touches EntityStateHandle.
     */
    private void writeMovementState(Vector3Int input) {

        EntityStateHandle state = player.getEntityStateHandle();

        // Never override vertical state
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

    public EntityHandle getPlayer() {
        return player;
    }

    public WorldPositionStruct getPlayerPosition() {
        return player.getWorldPositionStruct();
    }
}