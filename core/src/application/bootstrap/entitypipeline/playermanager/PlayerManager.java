package application.bootstrap.entitypipeline.playermanager;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.entity.EntityState;
import application.bootstrap.entitypipeline.entity.EntityStateHandle;
import application.bootstrap.entitypipeline.entitymanager.EntityManager;
import application.bootstrap.entitypipeline.placementmanager.PlacementManager;
import application.bootstrap.inputpipeline.input.InputHandle;
import application.bootstrap.inputpipeline.inputsystem.InputSystem;
import application.bootstrap.physicspipeline.movementmanager.MovementManager;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.chunk.ChunkData;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.util.WorldPositionStruct;
import application.bootstrap.worldpipeline.util.WorldPositionUtility;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import application.kernel.util.window.WindowInstance;
import engine.root.ManagerPackage;
import engine.settings.EngineSetting;
import engine.util.camera.CameraInstance;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

public class PlayerManager extends ManagerPackage {

    /*
     * Owns and drives the player entity and its camera. spawnPlayer() takes the
     * window the player renders into — caller decides, no internal window lookup.
     * Camera position is synchronized to the player eye position each frame.
     * Camera rotation is driven externally by PlayerInputSystem.
     */

    // Internal
    private InputSystem inputSystem;
    private MovementManager movementManager;
    private EntityManager entityManager;
    private BlockManager blockManager;
    private WorldStreamManager worldStreamManager;

    // Systems
    private InternalBufferSystem internalBufferSystem;
    private PlacementManager placementManager;

    // Player + Camera (per window)
    private Int2ObjectOpenHashMap<EntityInstance> windowID2Player;
    private Int2ObjectOpenHashMap<CameraInstance> windowID2Camera;
    private Int2BooleanOpenHashMap windowID2VerifyPlayerPosition;

    // Scratch
    private Vector3 cameraPosition;
    private Vector3 cameraOffset;

    // Internal \\

    @Override
    protected void create() {

        // Systems
        this.internalBufferSystem = create(InternalBufferSystem.class);
        this.placementManager = create(PlacementManager.class);

        // Player + Camera
        this.windowID2Player = new Int2ObjectOpenHashMap<>();
        this.windowID2Camera = new Int2ObjectOpenHashMap<>();
        this.windowID2VerifyPlayerPosition = new Int2BooleanOpenHashMap();

        // Scratch
        this.cameraPosition = new Vector3();
        this.cameraOffset = new Vector3();
    }

    @Override
    protected void get() {

        // Internal
        this.inputSystem = get(InputSystem.class);
        this.movementManager = get(MovementManager.class);
        this.entityManager = get(EntityManager.class);
        this.blockManager = get(BlockManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
    }

    @Override
    protected void update() {

        if (windowID2Player.isEmpty())
            return;

        for (Int2ObjectMap.Entry<EntityInstance> entry : windowID2Player.int2ObjectEntrySet()) {
            int windowID = entry.getIntKey();
            EntityInstance player = entry.getValue();
            CameraInstance camera = windowID2Camera.get(windowID);

            if (camera == null)
                continue;

            calculatePlayerPosition(windowID, player, camera);
        }
    }

    // Spawn \\

    public EntityInstance spawnPlayer(WindowInstance window) {

        EntityInstance player = entityManager.spawnEntity(EngineSetting.DEFAULT_PLAYER_RACE);
        windowID2Player.put(window.getWindowID(), player);
        windowID2VerifyPlayerPosition.put(window.getWindowID(), true);
        windowID2Camera.put(window.getWindowID(), window.getActiveCamera());

        return player;
    }

    // Player \\

    private void calculatePlayerPosition(int windowID, EntityInstance player, CameraInstance camera) {

        WorldPositionStruct worldPositionStruct = player.getWorldPositionStruct();
        boolean verifyPlayerPosition = windowID2VerifyPlayerPosition.get(windowID);

        if (verifyPlayerPosition) {
            verifyPlayerPosition = verifyPlayerPosition(player, worldPositionStruct);
            windowID2VerifyPlayerPosition.put(windowID, verifyPlayerPosition);
            return;
        }

        writeMovementState(player);
        movementManager.move(player);

        cameraOffset.set(
                player.getSize().x / 2,
                player.getEyeHeight(),
                player.getSize().z / 2);

        cameraPosition.set(worldPositionStruct.getPosition());
        cameraPosition.add(cameraOffset);
        camera.setPosition(cameraPosition);

        placementManager.update(
                player,
                cameraPosition,
                camera.getDirection(),
                inputSystem.isLeftDown(),
                inputSystem.isRightDown());

        internalBufferSystem.updatePlayerPosition(worldPositionStruct);
    }

    private void writeMovementState(EntityInstance player) {

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

    private boolean verifyPlayerPosition(EntityInstance player, WorldPositionStruct worldPositionStruct) {

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

    public EntityInstance getPlayerForWindow(int windowID) {
        return windowID2Player.get(windowID);
    }

    public boolean hasPlayerForWindow(int windowID) {
        return windowID2Player.containsKey(windowID);
    }

    public CameraInstance getCameraForWindow(int windowID) {
        return windowID2Camera.get(windowID);
    }

    public WorldPositionStruct getPlayerPositionForWindow(int windowID) {
        EntityInstance player = windowID2Player.get(windowID);
        return player == null ? null : player.getWorldPositionStruct();
    }
}