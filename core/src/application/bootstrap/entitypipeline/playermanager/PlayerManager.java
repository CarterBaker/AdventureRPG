package application.bootstrap.entitypipeline.playermanager;

import application.bootstrap.combatpipeline.combatmanager.CombatManager;
import application.bootstrap.entitypipeline.entity.EntityInputHandle;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.entity.EntityState;
import application.bootstrap.entitypipeline.entity.EntityStateHandle;
import application.bootstrap.entitypipeline.entitymanager.EntityManager;
import application.bootstrap.entitypipeline.placementmanager.PlacementManager;
import application.bootstrap.entitypipeline.player.PlayerCameraStruct;
import application.bootstrap.entitypipeline.player.PlayerGestureStruct;
import application.bootstrap.entitypipeline.player.UseTargetStruct;
import application.bootstrap.physicspipeline.movementmanager.MovementManager;
import application.bootstrap.vehiclepipeline.vehiclemanager.VehicleCargoSystem;
import application.bootstrap.vehiclepipeline.vehiclemanager.VehicleControlSystem;
import application.bootstrap.vehiclepipeline.vehiclemanager.VehicleManager;
import application.bootstrap.vehiclepipeline.vehiclemanager.VehicleRiderSystem;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.chunk.ChunkData;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.util.WorldPositionStruct;
import application.bootstrap.worldpipeline.util.WorldPositionUtility;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.worlditem.WorldItemInstance;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import application.kernel.inputpipeline.input.RawInputHandle;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import engine.assets.camera.CameraInstance;
import engine.input.Keys;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class PlayerManager extends ManagerPackage {

    /*
     * Owns and drives each window's player entity and camera. The camera trails
     * the eye at a smoothed third-person distance that scroll and the
     * first-person toggle adjust; gameplay always aims from the eye. Closing
     * in, FirstPersonBranch seats the camera at the posed head's eyes and
     * bends and twists the chest after the look, the feet staying planted
     * while a still player looks around, and CameraShakeBranch shakes the
     * view with the jolts the player takes while it is in play. Only the
     * hovered window's player updates, gated on its menu lock. Also handles
     * free-camera flight, rerolls, spawn verification, and character-creator
     * previews, and advances the player's animation after movement each frame.
     * despawnPlayer() mirrors spawnPlayer(), forgetting every per-window entry
     * and taking the player out of the world when its window closes.
     * After movement the player's input reaches the world through
     * PlacementManager and its fighting — attacks, aiming and blocking —
     * through CombatManager, the side of each read from the mouse by
     * PlayerGestureSystem; a raised guard or a stagger holds the player to a
     * walk and an attack keeps it from sprinting. While a gesture reads, the
     * view holds still, and behind a raised guard it turns slower, so a
     * swipe steers the weapon instead of the camera. Aboard a
     * vehicle the view turns with the deck, a player at the helm steers with
     * its sideways input, and the activate binding works the vehicle control
     * the player faces before it reaches PlacementManager. A teleported
     * player lets go of any vehicle and is dropped onto the ground of the
     * column it was sent to, by the same check that verifies a spawn.
     */

    // Internal
    private MovementManager movementManager;
    private EntityManager entityManager;
    private BlockManager blockManager;
    private WorldStreamManager worldStreamManager;
    private WindowManager windowManager;

    // Systems
    private PlayerInputSystem playerInputSystem;
    private PlayerBufferSystem internalBufferSystem;
    private FirstPersonBranch firstPersonBranch;
    private CameraShakeBranch cameraShakeBranch;
    private PlacementManager placementManager;
    private CombatManager combatManager;
    private VehicleControlSystem vehicleControlSystem;
    private VehicleRiderSystem vehicleRiderSystem;
    private VehicleManager vehicleManager;
    private VehicleCargoSystem vehicleCargoSystem;

    // Per-window
    private Int2ObjectOpenHashMap<EntityInstance> windowID2Player;
    private Int2ObjectOpenHashMap<CameraInstance> windowID2Camera;
    private Int2ObjectOpenHashMap<RawInputHandle> windowID2RawInput;
    private Int2ObjectOpenHashMap<WindowInstance> windowID2Window;
    private Int2ObjectOpenHashMap<PlayerGestureStruct> windowID2Gesture;
    private Int2ObjectOpenHashMap<PlayerCameraStruct> windowID2CameraState;
    private Int2BooleanOpenHashMap windowID2VerifyPlayerPosition;
    private Int2BooleanOpenHashMap windowID2FreeCamera;

    // Per-window zoom
    private Int2FloatOpenHashMap windowID2ZoomDistance;
    private Int2FloatOpenHashMap windowID2ZoomTarget;

    // Per-window first-person toggle (F5) — snapshot/restore of the third
    // person distance the camera was at before the most recent F5 press.
    private Int2BooleanOpenHashMap windowID2FirstPersonToggled;
    private Int2FloatOpenHashMap windowID2PreFirstPersonZoomTarget;

    // Per-window character preview
    private Int2BooleanOpenHashMap windowID2CharacterPreview;
    private Int2FloatOpenHashMap windowID2CharacterPreviewYaw;

    // Per-window preview framing — x, y: the screen point in NDC the character centres on;
    // z: the share of the screen height the character fills
    private Int2ObjectOpenHashMap<Vector3> windowID2PreviewFraming;

    // Scratch
    private Vector3 cameraPosition;
    private Vector3 cameraOffset;
    private Vector3 eyePosition;
    private Vector3 facingDirection;
    private Vector2 turnScratch;

    // Internal \\

    @Override
    protected void create() {

        this.playerInputSystem = create(PlayerInputSystem.class);
        create(PlayerGestureSystem.class);
        this.internalBufferSystem = create(PlayerBufferSystem.class);
        this.firstPersonBranch = create(FirstPersonBranch.class);
        this.cameraShakeBranch = create(CameraShakeBranch.class);
        this.placementManager = create(PlacementManager.class);

        this.windowID2Player = new Int2ObjectOpenHashMap<>();
        this.windowID2Camera = new Int2ObjectOpenHashMap<>();
        this.windowID2RawInput = new Int2ObjectOpenHashMap<>();
        this.windowID2Window = new Int2ObjectOpenHashMap<>();
        this.windowID2Gesture = new Int2ObjectOpenHashMap<>();
        this.windowID2CameraState = new Int2ObjectOpenHashMap<>();
        this.windowID2VerifyPlayerPosition = new Int2BooleanOpenHashMap();
        this.windowID2FreeCamera = new Int2BooleanOpenHashMap();

        this.windowID2ZoomDistance = new Int2FloatOpenHashMap();
        this.windowID2ZoomTarget = new Int2FloatOpenHashMap();

        this.windowID2FirstPersonToggled = new Int2BooleanOpenHashMap();
        this.windowID2PreFirstPersonZoomTarget = new Int2FloatOpenHashMap();

        this.windowID2CharacterPreview = new Int2BooleanOpenHashMap();
        this.windowID2CharacterPreviewYaw = new Int2FloatOpenHashMap();
        this.windowID2PreviewFraming = new Int2ObjectOpenHashMap<>();

        this.cameraPosition = new Vector3();
        this.cameraOffset = new Vector3();
        this.eyePosition = new Vector3();
        this.facingDirection = new Vector3();
        this.turnScratch = new Vector2();
    }

    @Override
    protected void get() {
        this.movementManager = get(MovementManager.class);
        this.entityManager = get(EntityManager.class);
        this.blockManager = get(BlockManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
        this.windowManager = get(WindowManager.class);
        this.combatManager = get(CombatManager.class);
        this.vehicleControlSystem = get(VehicleControlSystem.class);
        this.vehicleRiderSystem = get(VehicleRiderSystem.class);
        this.vehicleManager = get(VehicleManager.class);
        this.vehicleCargoSystem = get(VehicleCargoSystem.class);
    }

    @Override
    protected void update() {

        if (windowID2Player.isEmpty())
            return;

        WindowInstance activeWindow = windowManager.getHoveredWindow();

        if (activeWindow == null)
            return;

        int windowID = activeWindow.getWindowID();
        EntityInstance player = windowID2Player.get(windowID);
        CameraInstance camera = windowID2Camera.get(windowID);
        RawInputHandle raw = windowID2RawInput.get(windowID);

        if (player == null || camera == null || raw == null)
            return;

        calculatePlayerPosition(windowID, player, camera, raw);
    }

    // Spawn \\

    public EntityInstance spawnPlayer(WindowInstance window, RawInputHandle rawInput) {
        EntityInstance player = entityManager.spawnEntity(EngineSetting.DEFAULT_PLAYER_RACE);
        int windowID = window.getWindowID();
        windowID2Player.put(windowID, player);
        windowID2Camera.put(windowID, window.getActiveCamera());
        windowID2RawInput.put(windowID, rawInput);
        windowID2Window.put(windowID, window);
        windowID2Gesture.put(windowID, new PlayerGestureStruct());
        windowID2CameraState.put(windowID, new PlayerCameraStruct());
        windowID2VerifyPlayerPosition.put(windowID, true);
        windowID2FreeCamera.put(windowID, false);
        windowID2ZoomDistance.put(windowID, EngineSetting.CAMERA_ZOOM_DEFAULT);
        windowID2ZoomTarget.put(windowID, EngineSetting.CAMERA_ZOOM_DEFAULT);
        windowID2FirstPersonToggled.put(windowID, false);
        windowID2PreFirstPersonZoomTarget.put(windowID, EngineSetting.CAMERA_ZOOM_DEFAULT);
        windowID2CharacterPreview.put(windowID, false);
        windowID2CharacterPreviewYaw.put(windowID, 0f);
        return player;
    }

    public void despawnPlayer(WindowInstance window) {

        int windowID = window.getWindowID();
        EntityInstance player = windowID2Player.remove(windowID);

        windowID2Camera.remove(windowID);
        windowID2RawInput.remove(windowID);
        windowID2Window.remove(windowID);
        windowID2Gesture.remove(windowID);
        windowID2CameraState.remove(windowID);
        windowID2VerifyPlayerPosition.remove(windowID);
        windowID2FreeCamera.remove(windowID);
        windowID2ZoomDistance.remove(windowID);
        windowID2ZoomTarget.remove(windowID);
        windowID2FirstPersonToggled.remove(windowID);
        windowID2PreFirstPersonZoomTarget.remove(windowID);
        windowID2CharacterPreview.remove(windowID);
        windowID2CharacterPreviewYaw.remove(windowID);
        windowID2PreviewFraming.remove(windowID);

        if (player != null)
            entityManager.despawnEntity(player);
    }

    // Character \\

    public void rerollPlayerForWindow(int windowID) {
        entityManager.rerollEntity(windowID2Player.get(windowID));
        verifyPlayerPositionForWindow(windowID);
    }

    public void verifyPlayerPositionForWindow(int windowID) {
        windowID2VerifyPlayerPosition.put(windowID, true);
    }

    public void setFreeCameraForWindow(int windowID, boolean freeCamera) {

        if (windowID2FreeCamera.get(windowID) == freeCamera)
            return;

        windowID2FreeCamera.put(windowID, freeCamera);

        if (!freeCamera)
            verifyPlayerPositionForWindow(windowID);
    }

    // Teleport \\

    // The window's player stands at the top of the column given and settles onto the ground once its chunk streams in
    public void teleportPlayerForWindow(int windowID, long chunkCoordinate, int blockX, int blockZ) {

        EntityInstance player = windowID2Player.get(windowID);

        if (player == null)
            return;

        WorldPositionStruct worldPositionStruct = player.getWorldPositionStruct();
        EntityStateHandle state = player.getEntityStateHandle();

        vehicleManager.releaseEntity(player);
        worldPositionStruct.getPosition().set(
                blockX,
                EngineSetting.WORLD_HEIGHT * EngineSetting.CHUNK_SIZE - 1,
                blockZ);
        worldPositionStruct.setChunkCoordinate(
                WorldWrapUtility.wrapAroundWorld(player.getWorldHandle(), chunkCoordinate));
        state.getGravityVelocity().set(0f);
        state.getHorizontalVelocity().set(0f);

        verifyPlayerPositionForWindow(windowID);
        internalBufferSystem.updatePlayerPosition(worldPositionStruct);
    }

    // Player \\

    private void calculatePlayerPosition(
            int windowID,
            EntityInstance player,
            CameraInstance camera,
            RawInputHandle raw) {

        WorldPositionStruct worldPositionStruct = player.getWorldPositionStruct();
        PlayerCameraStruct cameraState = windowID2CameraState.get(windowID);
        boolean verifyPlayerPosition = windowID2VerifyPlayerPosition.get(windowID);

        if (verifyPlayerPosition) {
            cameraShakeBranch.calm(camera, cameraState);
            verifyPlayerPosition = verifyPlayerPosition(player, worldPositionStruct);
            windowID2VerifyPlayerPosition.put(windowID, verifyPlayerPosition);
            return;
        }

        PlayerGestureStruct gesture = windowID2Gesture.get(windowID);

        if (windowID2CharacterPreview.get(windowID)) {
            gesture.reset();
            cameraShakeBranch.calm(camera, cameraState);
            updateCharacterPreview(windowID, player, camera);
            return;
        }

        turnWithDeck(player, camera);

        if (windowID2Window.get(windowID).getMenuListHandle().isInputLocked()) {
            gesture.reset();
            cameraShakeBranch.calm(camera, cameraState);
            return;
        }

        // Translate raw hardware → game intent before anything reads EntityInputHandle
        EntityInputHandle input = player.getEntityInputHandle();
        boolean firstPerson = isFirstPerson(windowID);

        playerInputSystem.translate(raw, input, gesture, combatManager.isArmed(player));
        input.setStrafe(firstPerson);
        input.setTwist(firstPerson && !combatManager.isEngaged(player));

        if (windowID2FreeCamera.get(windowID)) {
            cameraShakeBranch.calm(camera, cameraState);
            calculateFreeCameraPosition(player, camera);
            return;
        }

        vehicleControlSystem.steer(player);
        movementManager.writeMovementState(player, combatManager.isSlowed(player), combatManager.isAttacking(player));
        movementManager.move(player);

        float distance = updateZoom(windowID, raw);
        float eyeBlend = firstPersonBranch.resolveEyeBlend(distance);

        firstPersonBranch.pose(player, eyeBlend);
        player.updateAnimation(internal.getDeltaTime());

        // Eye position — where gameplay (aiming, raycasts) actually happens,
        // regardless of where the visual camera ends up.
        resolveEyePosition(player);

        // Visual camera — seated at the posed head's eyes as it closes in on
        // first person, and pulled back behind them along the view direction.
        Vector3 direction = camera.getDirection();

        firstPersonBranch.resolveCameraOrigin(player, eyeBlend, eyePosition, cameraPosition);
        cameraPosition.subtract(
                direction.x * distance,
                direction.y * distance,
                direction.z * distance);

        camera.setPosition(cameraPosition);
        cameraShakeBranch.update(player, camera, cameraState);

        boolean activated = input.isActivateAction()
                && vehicleControlSystem.activate(player, eyePosition, camera.getDirection());

        placementManager.update(
                player,
                eyePosition,
                camera.getDirection(),
                input.isPrimaryAction(),
                input.isActivateAction() && !activated);
        combatManager.control(player);

        internalBufferSystem.updatePlayerPosition(worldPositionStruct);
    }

    private void calculateFreeCameraPosition(EntityInstance anchor, CameraInstance camera) {

        movementManager.fly(anchor);

        resolveEyePosition(anchor);
        camera.setPosition(eyePosition);

        internalBufferSystem.updatePlayerPosition(anchor.getWorldPositionStruct());
    }

    private void resolveEyePosition(EntityInstance player) {
        player.getEyePosition(eyePosition);
    }

    // The view turns with the deck a rider stands on, by however far the deck has turned since the last frame
    private void turnWithDeck(EntityInstance player, CameraInstance camera) {

        float turn = vehicleRiderSystem.takeTurn(player);

        if (turn != 0f)
            camera.setRotation(turnScratch.set(-(float) Math.toDegrees(turn), 0f));
    }

    // Character Preview \\

    public void beginCharacterPreview(int windowID) {

        Vector3 direction = windowID2Camera.get(windowID).getDirection();

        windowID2CharacterPreviewYaw.put(windowID, (float) Math.atan2(-direction.x, -direction.z));
        windowID2CharacterPreview.put(windowID, true);
    }

    public void endCharacterPreview(int windowID) {

        if (!windowID2CharacterPreview.get(windowID))
            return;

        windowID2CharacterPreview.put(windowID, false);
        windowID2PreviewFraming.remove(windowID);
        windowID2Camera.get(windowID).setDirection(resolvePreviewFacing(windowID));
    }

    public void frameCharacterPreview(int windowID, float centerX, float centerY, float heightShare) {

        Vector3 framing = windowID2PreviewFraming.get(windowID);

        if (framing == null) {
            framing = new Vector3();
            windowID2PreviewFraming.put(windowID, framing);
        }

        framing.set(centerX, centerY, heightShare);
    }

    public boolean isCharacterPreview(int windowID) {
        return windowID2CharacterPreview.get(windowID);
    }

    public void rotateCharacterPreview(int windowID, float degrees) {
        windowID2CharacterPreviewYaw.put(
                windowID,
                windowID2CharacterPreviewYaw.get(windowID) + (float) Math.toRadians(degrees));
    }

    private Vector3 resolvePreviewFacing(int windowID) {

        float yaw = windowID2CharacterPreviewYaw.get(windowID);

        return facingDirection.set((float) Math.sin(yaw), 0f, (float) Math.cos(yaw));
    }

    // The camera looks back at a point beside the character; its right axis along -facing is (facing.z, 0, -facing.x)
    private void updateCharacterPreview(int windowID, EntityInstance player, CameraInstance camera) {

        Vector3 facing = resolvePreviewFacing(windowID);
        EntityStateHandle state = player.getEntityStateHandle();
        EntityInputHandle input = player.getEntityInputHandle();

        state.setMovementState(EntityState.IDLE);
        state.setSpeed(0f, 0f);
        input.setFacingDirection(facing.x, 0f, facing.z);
        input.setStrafe(true);
        input.setTwist(false);

        movementManager.face(player);
        firstPersonBranch.pose(player, 0f);
        player.updateAnimation(internal.getDeltaTime());

        Vector3 framing = windowID2PreviewFraming.get(windowID);

        if (framing != null) {
            placeFramedPreviewCamera(player, camera, facing, framing);
            internalBufferSystem.updatePlayerPosition(player.getWorldPositionStruct());
            return;
        }

        Vector3 size = player.getSize();
        float lateral = EngineSetting.CHARACTER_PREVIEW_LATERAL_OFFSET;
        float distance = EngineSetting.CHARACTER_PREVIEW_DISTANCE;
        float lift = EngineSetting.CHARACTER_PREVIEW_LIFT;

        cameraPosition.set(player.getWorldPositionStruct().getPosition());
        cameraPosition.add(
                size.x * 0.5f - facing.z * lateral,
                size.y * EngineSetting.CHARACTER_PREVIEW_FOCUS_HEIGHT,
                size.z * 0.5f + facing.x * lateral);
        cameraPosition.add(facing.x * distance, lift, facing.z * distance);

        cameraOffset.set(-facing.x * distance, -lift, -facing.z * distance);

        camera.setPosition(cameraPosition);
        camera.setDirection(cameraOffset);

        internalBufferSystem.updatePlayerPosition(player.getWorldPositionStruct());
    }

    private void placeFramedPreviewCamera(
            EntityInstance player,
            CameraInstance camera,
            Vector3 facing,
            Vector3 framing) {

        Vector3 size = player.getSize();
        float tanHalfFov = (float) Math.tan(Math.toRadians(camera.getFOV()) * 0.5);
        float aspect = camera.getViewport().x / Math.max(1f, camera.getViewport().y);
        float distance = size.y / (framing.z * 2f * tanHalfFov);
        float lateral = framing.x * distance * tanHalfFov * aspect;
        float vertical = framing.y * distance * tanHalfFov;

        cameraPosition.set(player.getWorldPositionStruct().getPosition());
        cameraPosition.add(
                size.x * 0.5f - facing.z * lateral,
                size.y * EngineSetting.CHARACTER_PREVIEW_CENTER_HEIGHT - vertical,
                size.z * 0.5f + facing.x * lateral);
        cameraPosition.add(facing.x * distance, 0f, facing.z * distance);

        cameraOffset.set(-facing.x, 0f, -facing.z);

        camera.setPosition(cameraPosition);
        camera.setDirection(cameraOffset);
    }

    // Placement \\

    public boolean placeBlockForWindow(int windowID, short blockID) {

        EntityInstance player = windowID2Player.get(windowID);
        CameraInstance camera = windowID2Camera.get(windowID);

        if (player == null || camera == null)
            return false;

        resolveEyePosition(player);

        return placementManager.placeBlock(player, eyePosition, camera.getDirection(), blockID);
    }

    public boolean placeSubBlockForWindow(int windowID, short blockID) {

        EntityInstance player = windowID2Player.get(windowID);
        CameraInstance camera = windowID2Camera.get(windowID);

        if (player == null || camera == null)
            return false;

        resolveEyePosition(player);

        return placementManager.placeSubBlock(player, eyePosition, camera.getDirection(), blockID);
    }

    // True while the window's player faces a vehicle control it can work, or holds a helm it can let go
    public boolean isFacingVehicleControlForWindow(int windowID) {

        EntityInstance player = windowID2Player.get(windowID);
        CameraInstance camera = windowID2Camera.get(windowID);

        if (player == null || camera == null)
            return false;

        resolveEyePosition(player);

        return vehicleControlSystem.isSteering(player)
                || vehicleControlSystem.findControl(player, eyePosition, camera.getDirection());
    }

    // True when the window's player aims at a vehicle nearer than anything in the world — VehicleCargoSystem keeps
    // the hit
    public boolean isAimingAtVehicleForWindow(int windowID) {

        EntityInstance player = windowID2Player.get(windowID);
        CameraInstance camera = windowID2Camera.get(windowID);

        if (player == null || camera == null)
            return false;

        resolveEyePosition(player);

        return placementManager.findTargetVehicle(player, eyePosition, camera.getDirection());
    }

    // The item the window's player would use with the activate binding — cargo when it aims at a vehicle, a world
    // item otherwise — written to the target given; false when it faces nothing it could use
    public boolean findUseTargetForWindow(int windowID, UseTargetStruct out) {

        out.clear();

        if (isAimingAtVehicleForWindow(windowID)) {

            if (vehicleCargoSystem.isCargoHit())
                out.setCargo(vehicleCargoSystem.getCastVehicle(), vehicleCargoSystem.getCastCargo());

            return out.hasTarget();
        }

        WorldItemInstance targetItem = getTargetItemForWindow(windowID);

        if (targetItem != null)
            out.setWorldItem(targetItem);

        return out.hasTarget();
    }

    public WorldItemInstance getTargetItemForWindow(int windowID) {

        EntityInstance player = windowID2Player.get(windowID);
        CameraInstance camera = windowID2Camera.get(windowID);

        if (player == null || camera == null)
            return null;

        resolveEyePosition(player);

        return placementManager.findTargetItem(player, eyePosition, camera.getDirection());
    }

    // Zoom \\

    private float updateZoom(int windowID, RawInputHandle raw) {

        float target = windowID2ZoomTarget.get(windowID);

        target -= raw.getScrollY() * EngineSetting.CAMERA_ZOOM_SCROLL_SPEED;
        target = Math.max(EngineSetting.CAMERA_ZOOM_MIN, Math.min(EngineSetting.CAMERA_ZOOM_MAX, target));

        if (raw.isKeyClicked(Keys.F5))
            target = toggleFirstPerson(windowID, target);

        windowID2ZoomTarget.put(windowID, target);

        float current = windowID2ZoomDistance.get(windowID);
        float smoothing = Math.min(1f, internal.getDeltaTime() * EngineSetting.CAMERA_ZOOM_SMOOTHING);
        current += (target - current) * smoothing;

        windowID2ZoomDistance.put(windowID, current);

        return current;
    }

    private float toggleFirstPerson(int windowID, float currentTarget) {

        boolean firstPersonToggled = windowID2FirstPersonToggled.get(windowID);

        if (!firstPersonToggled) {
            windowID2PreFirstPersonZoomTarget.put(windowID, currentTarget);
            windowID2FirstPersonToggled.put(windowID, true);
            return EngineSetting.CAMERA_ZOOM_MIN;
        }

        windowID2FirstPersonToggled.put(windowID, false);
        return windowID2PreFirstPersonZoomTarget.get(windowID);
    }

    public boolean isFirstPerson(int windowID) {
        return !windowID2CharacterPreview.get(windowID)
                && windowID2ZoomDistance.get(windowID) <= EngineSetting.CAMERA_FIRST_PERSON_THRESHOLD;
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

        if (safeY == totalY)
            return false;

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

    public boolean isFreeCameraForWindow(int windowID) {
        return windowID2FreeCamera.get(windowID);
    }

    // True while the window's player is in play — not flying free, not posed for a preview, no menu holding input
    public boolean isPlayingForWindow(int windowID) {

        WindowInstance window = windowID2Window.get(windowID);

        return window != null
                && !windowID2FreeCamera.get(windowID)
                && !windowID2CharacterPreview.get(windowID)
                && !window.getMenuListHandle().isInputLocked();
    }

    public CameraInstance getCameraForWindow(int windowID) {
        return windowID2Camera.get(windowID);
    }

    // How much of the mouse's travel turns the window's view — none while a gesture reads, less behind a guard
    public float getLookScaleForWindow(int windowID) {

        PlayerGestureStruct gesture = windowID2Gesture.get(windowID);
        EntityInstance player = windowID2Player.get(windowID);

        if (gesture == null || player == null)
            return EngineSetting.LOOK_SCALE_FULL;

        if (gesture.isReading())
            return EngineSetting.GESTURE_READ_LOOK_SCALE;

        return combatManager.isGuarding(player) ? EngineSetting.GUARD_LOOK_SCALE : EngineSetting.LOOK_SCALE_FULL;
    }

    public WorldPositionStruct getPlayerPositionForWindow(int windowID) {
        EntityInstance player = windowID2Player.get(windowID);
        return player == null ? null : player.getWorldPositionStruct();
    }

    public void pushPlayerPositionForWindow(int windowID) {
        WorldPositionStruct position = getPlayerPositionForWindow(windowID);
        if (position == null)
            return;
        internalBufferSystem.updatePlayerPosition(position);
    }

    public void setInputLocked(boolean locked) {
        playerInputSystem.setInputLocked(locked);
    }
}