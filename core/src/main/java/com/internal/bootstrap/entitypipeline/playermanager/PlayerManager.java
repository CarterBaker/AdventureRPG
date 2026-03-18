package com.internal.bootstrap.entitypipeline.playermanager;

import com.internal.bootstrap.entitypipeline.entity.EntityInstance;
import com.internal.bootstrap.entitypipeline.entity.EntityState;
import com.internal.bootstrap.entitypipeline.entity.EntityStateHandle;
import com.internal.bootstrap.entitypipeline.entitymanager.EntityManager;
import com.internal.bootstrap.entitypipeline.placementmanager.PlacementManager;
import com.internal.bootstrap.inputpipeline.input.InputHandle;
import com.internal.bootstrap.inputpipeline.inputsystem.InputSystem;
import com.internal.bootstrap.physicspipeline.movementmanager.MovementManager;
import com.internal.bootstrap.renderpipeline.camera.CameraInstance;
import com.internal.bootstrap.renderpipeline.cameramanager.CameraManager;
import com.internal.bootstrap.renderpipeline.window.WindowInstance;
import com.internal.bootstrap.worldpipeline.blockmanager.BlockManager;
import com.internal.bootstrap.worldpipeline.chunk.ChunkData;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.util.WorldPositionStruct;
import com.internal.bootstrap.worldpipeline.util.WorldPositionUtility;
import com.internal.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.mathematics.vectors.Vector3;

public class PlayerManager extends ManagerPackage {

    /*
     * Owns and drives the player entity and its camera. spawnPlayer() takes the
     * window the player renders into — caller decides, no internal window lookup.
     * Camera position is synchronized to the player eye position each frame.
     * Camera rotation is driven externally by PlayerInputSystem.
     */

    // Internal
    private CameraManager cameraManager;
    private InputSystem inputSystem;
    private MovementManager movementManager;
    private EntityManager entityManager;
    private BlockManager blockManager;
    private WorldStreamManager worldStreamManager;

    // Systems
    private InternalBufferSystem internalBufferSystem;
    private PlacementManager placementManager;

    // Player
    private EntityInstance player;
    private boolean verifyPlayerPosition;
    private Vector3 cameraPosition;
    private Vector3 cameraOffset;

    // Camera
    private CameraInstance camera;

    // Internal \\

    @Override
    protected void create() {

        // Systems
        this.internalBufferSystem = create(InternalBufferSystem.class);
        this.placementManager = create(PlacementManager.class);

        // Player
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
        this.worldStreamManager = get(WorldStreamManager.class);
    }

    @Override
    protected void update() {

        if (player == null)
            return;

        calculatePlayerPosition();
    }

    // Spawn \\

    public EntityInstance spawnPlayer(WindowInstance window) {

        this.player = entityManager.spawnEntity(EngineSetting.DEFAULT_PLAYER_RACE);
        this.verifyPlayerPosition = true;
        this.cameraOffset.set(
                player.getSize().x / 2,
                player.getEyeHeight(),
                player.getSize().z / 2);

        this.camera = cameraManager.createCamera(
                internal.settings.FOV,
                window.getWidth(),
                window.getHeight());

        window.setActiveCamera(camera);

        return player;
    }

    // Player \\

    private void calculatePlayerPosition() {

        WorldPositionStruct worldPositionStruct = player.getWorldPositionStruct();

        if (verifyPlayerPosition) {
            verifyPlayerPosition = verifyPlayerPosition(worldPositionStruct);
            return;
        }

        writeMovementState();
        movementManager.move(player);

        cameraPosition.set(worldPositionStruct.getPosition());
        cameraPosition.add(cameraOffset);
        camera.setPosition(cameraPosition);

        placementManager.update(
                player,
                cameraPosition,
                camera.getDirection(),
                inputSystem.isLeftClick(),
                inputSystem.isRightClick());

        internalBufferSystem.updatePlayerPosition(worldPositionStruct);
    }

    private void writeMovementState() {

        EntityStateHandle state = player.getEntityStateHandle();
        InputHandle input = player.getInputHandle();

        if (!state.isGrounded())
            return;

        if (!input.hasHorizontalInput()) {
            state.setMovementState(EntityState.IDLE);
            return;
        }

        if (input.isWalk())
            state.setMovementState(EntityState.WALKING);
        else if (input.isSprint())
            state.setMovementState(EntityState.RUNNING);
        else
            state.setMovementState(EntityState.MOVING);
    }

    // Spawn Verification \\

    private boolean verifyPlayerPosition(WorldPositionStruct worldPositionStruct) {

        ChunkInstance activeChunkInstance = worldStreamManager.getChunkInstance(
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

    public boolean hasPlayer() {
        return player != null;
    }

    public CameraInstance getCamera() {
        return camera;
    }

    public WorldPositionStruct getPlayerPosition() {
        return player.getWorldPositionStruct();
    }
}