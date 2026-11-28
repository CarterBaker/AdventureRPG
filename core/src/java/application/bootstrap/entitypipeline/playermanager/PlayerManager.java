package application.bootstrap.entitypipeline.playermanager;

import application.bootstrap.animationpipeline.animation.AnimationClipHandle;
import application.bootstrap.combatpipeline.combatmanager.CombatManager;
import application.bootstrap.entitypipeline.animationtree.AnimationStateHandle;
import application.bootstrap.entitypipeline.entity.EntityInputHandle;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.entity.EntityState;
import application.bootstrap.entitypipeline.entity.EntityStateHandle;
import application.bootstrap.entitypipeline.entitymanager.EntityManager;
import application.bootstrap.entitypipeline.placementmanager.PlacementManager;
import application.bootstrap.entitypipeline.player.MenuViewStruct;
import application.bootstrap.entitypipeline.player.PlayerCameraStruct;
import application.bootstrap.entitypipeline.player.PlayerPhotoStruct;
import application.bootstrap.entitypipeline.player.UseTargetStruct;
import application.bootstrap.physicspipeline.movementmanager.MovementManager;
import application.bootstrap.renderpipeline.rendermanager.RenderManager;
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
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.world.WorldPlacementStruct;
import application.bootstrap.worldpipeline.worlditem.WorldItemInstance;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import application.kernel.inputpipeline.input.RawInputHandle;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import engine.assets.camera.CameraInstance;
import engine.input.Keys;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class PlayerManager extends ManagerPackage {

    /*
     * Owns and drives each window's player entity and camera. The camera
     * trails the eye at a smoothed third-person distance that scroll and the
     * first-person toggle adjust; gameplay always aims from the eye. Closing
     * in, FirstPersonBranch seats the camera at the posed head's eyes and
     * bends and twists the chest after the look, the feet staying planted
     * while a still player looks around, and CameraShakeBranch shakes the
     * view with the jolts the player takes while it is in play. Only the
     * hovered window's player updates, gated on its menu lock. Also handles
     * free-camera flight, rerolls, spawn verification, and character-creator
     * previews, and advances the player's animation after movement each
     * frame. despawnPlayer() mirrors spawnPlayer(), forgetting every
     * per-window entry and taking the player out of the world when its window
     * closes. After movement the player's input reaches the world through
     * PlacementManager and its fighting — attacks, aiming and blocking —
     * through CombatManager, the side of each read from the player's step by
     * PlayerGestureSystem; a raised guard or a stagger holds the player to a
     * walk and an attack keeps it from sprinting. Behind a raised guard the
     * view turns slower. Aboard a vehicle the view
     * turns with the deck, a player at the helm steers with its sideways
     * input, and the activate binding works the vehicle control the player
     * faces before it reaches PlacementManager. A teleported player lets go
     * of any vehicle and is dropped onto the ground of the column it was sent
     * to, by the same check that verifies a spawn. A held window's player and
     * camera are left exactly as something else places them — a recording
     * playing back — until the hold is let go. While a window's main menu
     * looks out over the world, hovered or not, MenuViewBranch seats its
     * camera at one of the world's hand-placed cameras, the player carried to
     * that camera's column and dropped onto its ground by the same check, so
     * the world streams around the view and the camera never starts inside
     * the ground. Each window's player position reaches its draw through a
     * window draw step added to RenderManager. A performing window's player
     * stands still with its hands held from the world, its input cleared each
     * frame, while it still falls, animates and looks around; it is not in
     * play, so nothing else reads its bindings.
     */

    // Internal
    private MovementManager movementManager;
    private EntityManager entityManager;
    private BlockManager blockManager;
    private WorldStreamManager worldStreamManager;
    private WindowManager windowManager;
    private RenderManager renderManager;

    // Systems
    private PlayerInputSystem playerInputSystem;
    private PlayerBufferSystem internalBufferSystem;
    private FirstPersonBranch firstPersonBranch;
    private CameraShakeBranch cameraShakeBranch;
    private MenuViewBranch menuViewBranch;
    private PhotoCameraBranch photoCameraBranch;
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
    private Int2ObjectOpenHashMap<PlayerCameraStruct> windowID2CameraState;
    private Int2BooleanOpenHashMap windowID2VerifyPlayerPosition;
    private Int2BooleanOpenHashMap windowID2FreeCamera;
    private Int2BooleanOpenHashMap windowID2Held;
    private Int2BooleanOpenHashMap windowID2Performing;

    // Per-window photo mode — its free camera, the pose held and whether the player is left out, while it is open
    private Int2ObjectOpenHashMap<PlayerPhotoStruct> windowID2Photo;

    // Per-window zoom
    private Int2FloatOpenHashMap windowID2ZoomDistance;
    private Int2FloatOpenHashMap windowID2ZoomTarget;

    // Per-window first-person toggle (F5) — snapshot/restore of the third
    // person distance the camera was at before the most recent F5 press.
    private Int2BooleanOpenHashMap windowID2FirstPersonToggled;
    private Int2FloatOpenHashMap windowID2PreFirstPersonZoomTarget;

    // Per-window character preview — the way the character faces, held while the preview is open, and how far the
    // camera has turned about it from there, so turning the preview moves the view, never the body or its hair
    private Int2BooleanOpenHashMap windowID2CharacterPreview;
    private Int2FloatOpenHashMap windowID2CharacterPreviewYaw;
    private Int2FloatOpenHashMap windowID2CharacterPreviewOrbit;

    // Per-window preview zoom — the bones the preview closes in on while a slider is worked, absent for the whole
    // body; how far it has closed in, 0 framing the whole body to 1 framing those bones; and the seconds since the
    // view was last turned
    private Int2ObjectOpenHashMap<int[]> windowID2PreviewFocus;
    private Int2FloatOpenHashMap windowID2PreviewZoom;
    private Int2FloatOpenHashMap windowID2PreviewTurnSeconds;

    // Per-window preview framing — x, y: the screen point in NDC the character centres on;
    // z: the share of the screen height the character fills
    private Int2ObjectOpenHashMap<Vector3> windowID2PreviewFraming;

    // Per-window menu view — the view each window's main menu looks out from, and every window holding one
    private Int2ObjectOpenHashMap<MenuViewStruct> windowID2MenuView;
    private IntArrayList menuViewWindowIDs;

    // Scratch
    private Vector3 cameraPosition;
    private Vector3 cameraOffset;
    private Vector3 eyePosition;
    private Vector3 facingDirection;
    private Vector3 viewDirection;
    private Vector3 focusCenter;
    private Vector3 focusPoint;
    private Vector3 focusCamera;
    private Vector2 turnScratch;

    // Internal \\

    @Override
    protected void create() {

        this.playerInputSystem = create(PlayerInputSystem.class);
        create(PlayerGestureSystem.class);
        this.internalBufferSystem = create(PlayerBufferSystem.class);
        this.firstPersonBranch = create(FirstPersonBranch.class);
        this.cameraShakeBranch = create(CameraShakeBranch.class);
        this.menuViewBranch = create(MenuViewBranch.class);
        this.photoCameraBranch = create(PhotoCameraBranch.class);
        this.placementManager = create(PlacementManager.class);

        this.windowID2Player = new Int2ObjectOpenHashMap<>();
        this.windowID2Camera = new Int2ObjectOpenHashMap<>();
        this.windowID2RawInput = new Int2ObjectOpenHashMap<>();
        this.windowID2Window = new Int2ObjectOpenHashMap<>();
        this.windowID2CameraState = new Int2ObjectOpenHashMap<>();
        this.windowID2VerifyPlayerPosition = new Int2BooleanOpenHashMap();
        this.windowID2FreeCamera = new Int2BooleanOpenHashMap();
        this.windowID2Held = new Int2BooleanOpenHashMap();
        this.windowID2Performing = new Int2BooleanOpenHashMap();
        this.windowID2Photo = new Int2ObjectOpenHashMap<>();

        this.windowID2ZoomDistance = new Int2FloatOpenHashMap();
        this.windowID2ZoomTarget = new Int2FloatOpenHashMap();

        this.windowID2FirstPersonToggled = new Int2BooleanOpenHashMap();
        this.windowID2PreFirstPersonZoomTarget = new Int2FloatOpenHashMap();

        this.windowID2CharacterPreview = new Int2BooleanOpenHashMap();
        this.windowID2CharacterPreviewYaw = new Int2FloatOpenHashMap();
        this.windowID2CharacterPreviewOrbit = new Int2FloatOpenHashMap();
        this.windowID2PreviewFocus = new Int2ObjectOpenHashMap<>();
        this.windowID2PreviewZoom = new Int2FloatOpenHashMap();
        this.windowID2PreviewTurnSeconds = new Int2FloatOpenHashMap();
        this.windowID2PreviewFraming = new Int2ObjectOpenHashMap<>();

        this.windowID2MenuView = new Int2ObjectOpenHashMap<>();
        this.menuViewWindowIDs = new IntArrayList();

        this.cameraPosition = new Vector3();
        this.cameraOffset = new Vector3();
        this.eyePosition = new Vector3();
        this.facingDirection = new Vector3();
        this.viewDirection = new Vector3();
        this.focusCenter = new Vector3();
        this.focusPoint = new Vector3();
        this.focusCamera = new Vector3();
        this.turnScratch = new Vector2();
    }

    @Override
    protected void get() {
        this.movementManager = get(MovementManager.class);
        this.entityManager = get(EntityManager.class);
        this.blockManager = get(BlockManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
        this.windowManager = get(WindowManager.class);
        this.renderManager = get(RenderManager.class);
        this.combatManager = get(CombatManager.class);
        this.vehicleControlSystem = get(VehicleControlSystem.class);
        this.vehicleRiderSystem = get(VehicleRiderSystem.class);
        this.vehicleManager = get(VehicleManager.class);
        this.vehicleCargoSystem = get(VehicleCargoSystem.class);
    }

    @Override
    protected void awake() {
        renderManager.addWindowDrawStep(this::pushPlayerPositionForWindow);
    }

    @Override
    protected void update() {

        if (windowID2Player.isEmpty())
            return;

        updateMenuViews();

        WindowInstance activeWindow = windowManager.getHoveredWindow();

        if (activeWindow == null)
            return;

        int windowID = activeWindow.getWindowID();
        EntityInstance player = windowID2Player.get(windowID);
        CameraInstance camera = windowID2Camera.get(windowID);
        RawInputHandle raw = windowID2RawInput.get(windowID);

        if (player == null || camera == null || raw == null || windowID2Held.get(windowID)
                || windowID2MenuView.containsKey(windowID))
            return;

        calculatePlayerPosition(windowID, player, camera, raw);
    }

    // Spawn \\

    public EntityInstance spawnPlayer(WindowInstance window, RawInputHandle rawInput, WorldHandle worldHandle) {
        EntityInstance player = entityManager.spawnEntity(EngineSetting.DEFAULT_PLAYER_RACE, worldHandle);
        int windowID = window.getWindowID();
        windowID2Player.put(windowID, player);
        windowID2Camera.put(windowID, window.getActiveCamera());
        windowID2RawInput.put(windowID, rawInput);
        windowID2Window.put(windowID, window);
        windowID2CameraState.put(windowID, new PlayerCameraStruct());
        windowID2VerifyPlayerPosition.put(windowID, true);
        windowID2FreeCamera.put(windowID, false);
        windowID2Held.put(windowID, false);
        windowID2Performing.put(windowID, false);
        windowID2ZoomDistance.put(windowID, EngineSetting.CAMERA_ZOOM_DEFAULT);
        windowID2ZoomTarget.put(windowID, EngineSetting.CAMERA_ZOOM_DEFAULT);
        windowID2FirstPersonToggled.put(windowID, false);
        windowID2PreFirstPersonZoomTarget.put(windowID, EngineSetting.CAMERA_ZOOM_DEFAULT);
        windowID2CharacterPreview.put(windowID, false);
        windowID2CharacterPreviewYaw.put(windowID, 0f);
        windowID2CharacterPreviewOrbit.put(windowID, 0f);
        windowID2PreviewZoom.put(windowID, 0f);
        windowID2PreviewTurnSeconds.put(windowID, EngineSetting.CHARACTER_PREVIEW_RELEASE_SECONDS);
        return player;
    }

    public void despawnPlayer(WindowInstance window) {

        int windowID = window.getWindowID();
        EntityInstance player = windowID2Player.remove(windowID);

        windowID2Camera.remove(windowID);
        windowID2RawInput.remove(windowID);
        windowID2Window.remove(windowID);
        windowID2CameraState.remove(windowID);
        windowID2VerifyPlayerPosition.remove(windowID);
        windowID2FreeCamera.remove(windowID);
        windowID2Held.remove(windowID);
        windowID2Performing.remove(windowID);
        windowID2Photo.remove(windowID);
        windowID2ZoomDistance.remove(windowID);
        windowID2ZoomTarget.remove(windowID);
        windowID2FirstPersonToggled.remove(windowID);
        windowID2PreFirstPersonZoomTarget.remove(windowID);
        windowID2CharacterPreview.remove(windowID);
        windowID2CharacterPreviewYaw.remove(windowID);
        windowID2CharacterPreviewOrbit.remove(windowID);
        windowID2PreviewFocus.remove(windowID);
        windowID2PreviewZoom.remove(windowID);
        windowID2PreviewTurnSeconds.remove(windowID);
        windowID2PreviewFraming.remove(windowID);
        endMenuView(windowID);

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

    // Photo Mode \\

    // The window's camera leaves the player where they stand and flies free near them until endPhotoMode()
    public void beginPhotoMode(int windowID) {

        CameraInstance camera = windowID2Camera.get(windowID);

        if (camera != null && !windowID2Photo.containsKey(windowID))
            windowID2Photo.put(windowID, new PlayerPhotoStruct(camera.getPosition()));
    }

    public void endPhotoMode(int windowID) {
        windowID2Photo.remove(windowID);
    }

    public boolean isPhotoModeForWindow(int windowID) {
        return windowID2Photo.containsKey(windowID);
    }

    // The pose clip the player holds in photo mode, null for the stance they were caught in; a clip of another rig is
    // refused, so each race and species holds only the poses made for its own
    public void setPhotoPose(int windowID, AnimationClipHandle pose) {

        PlayerPhotoStruct photo = windowID2Photo.get(windowID);
        EntityInstance player = windowID2Player.get(windowID);

        if (photo == null || player == null)
            return;

        photo.setPose(pose != null && pose.getRigHandle() == player.getAnimationStateHandle().getRigHandle()
                ? pose
                : null);
    }

    public void setPhotoPlayerHidden(int windowID, boolean hidden) {

        PlayerPhotoStruct photo = windowID2Photo.get(windowID);

        if (photo != null)
            photo.setPlayerHidden(hidden);
    }

    public boolean isPhotoPlayerHiddenForWindow(int windowID) {

        PlayerPhotoStruct photo = windowID2Photo.get(windowID);

        return photo != null && photo.isPlayerHidden();
    }

    // The player struck still in a pose, every worn piece on a rig of its own following it
    private void strikePhotoPose(EntityInstance player, AnimationClipHandle pose) {

        player.getAnimationStateHandle().poseClip(pose, 0f);

        if (player.getAppearanceHandle() != null)
            player.getAppearanceHandle().followPose(0f);
    }

    // The player held in their pose, or the stance they were caught in, and the camera flown while no menu holds input
    private void updatePhoto(
            PlayerPhotoStruct photo,
            EntityInstance player,
            CameraInstance camera,
            RawInputHandle raw,
            boolean inputLocked) {

        if (photo.getPose() != null)
            strikePhotoPose(player, photo.getPose());
        else
            player.updateAnimation(0f);

        resolveEyePosition(player);

        if (inputLocked) {
            player.getEntityInputHandle().clear();
            return;
        }

        playerInputSystem.translate(raw, player.getEntityInputHandle());
        photoCameraBranch.fly(player, photo, eyePosition, camera);
    }

    // Hold \\

    public void setHeldForWindow(int windowID, boolean held) {
        windowID2Held.put(windowID, held);
    }

    public boolean isHeldForWindow(int windowID) {
        return windowID2Held.get(windowID);
    }

    // Perform \\

    // The window's player stands still with its hands held from the world while it plays an instrument
    public void setPerformingForWindow(int windowID, boolean performing) {
        windowID2Performing.put(windowID, performing);
    }

    public boolean isPerformingForWindow(int windowID) {
        return windowID2Performing.get(windowID);
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
        worldStreamManager.restartChartFor(player);
        state.getGravityVelocity().set(0f);
        state.getHorizontalVelocity().set(0f);

        verifyPlayerPositionForWindow(windowID);
        internalBufferSystem.updatePlayerPosition(worldPositionStruct);
    }

    // The window's player stands at the top of a world block column, as teleportPlayerForWindow() stands it
    private void teleportPlayerToColumn(int windowID, long blockX, long blockZ) {
        teleportPlayerForWindow(
                windowID,
                Coordinate2Long.pack(
                        (int) Math.floorDiv(blockX, EngineSetting.CHUNK_SIZE),
                        (int) Math.floorDiv(blockZ, EngineSetting.CHUNK_SIZE)),
                (int) Math.floorMod(blockX, EngineSetting.CHUNK_SIZE),
                (int) Math.floorMod(blockZ, EngineSetting.CHUNK_SIZE));
    }

    // Menu View \\

    // The window's camera looks out from one of its world's cameras, chosen now, until endMenuView()
    public void beginMenuView(int windowID) {

        EntityInstance player = windowID2Player.get(windowID);

        if (player == null || windowID2MenuView.containsKey(windowID))
            return;

        windowID2MenuView.put(windowID, new MenuViewStruct(menuViewBranch.chooseCamera(player)));
        menuViewWindowIDs.add(windowID);
    }

    public void endMenuView(int windowID) {
        if (windowID2MenuView.remove(windowID) != null)
            menuViewWindowIDs.rem(windowID);
    }

    // Every menu view shows this camera while it is pinned, null letting each go back to its own
    public void pinMenuCamera(WorldPlacementStruct camera) {
        menuViewBranch.pin(camera);
    }

    public boolean isMenuViewForWindow(int windowID) {
        return windowID2MenuView.containsKey(windowID);
    }

    private void updateMenuViews() {
        for (int i = 0; i < menuViewWindowIDs.size(); i++)
            updateMenuView(menuViewWindowIDs.getInt(i));
    }

    // The player carried to the shown camera's column whenever it stands elsewhere, and the camera seated above it
    private void updateMenuView(int windowID) {

        if (windowID2Held.get(windowID))
            return;

        EntityInstance player = windowID2Player.get(windowID);
        CameraInstance camera = windowID2Camera.get(windowID);
        MenuViewStruct menuView = windowID2MenuView.get(windowID);
        WorldPlacementStruct shown = menuViewBranch.resolveCamera(menuView);

        if (!menuView.isAnchoredAt(shown.getWorldX(), shown.getWorldZ())) {
            teleportPlayerToColumn(windowID, shown.getWorldX(), shown.getWorldZ());
            menuView.anchor(shown.getWorldX(), shown.getWorldZ());
        }

        cameraShakeBranch.calm(camera, windowID2CameraState.get(windowID));

        if (windowID2VerifyPlayerPosition.get(windowID))
            windowID2VerifyPlayerPosition.put(
                    windowID,
                    verifyPlayerPosition(player, player.getWorldPositionStruct()));

        menuViewBranch.seat(player, camera, shown);
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

        if (windowID2CharacterPreview.get(windowID)) {
            cameraShakeBranch.calm(camera, cameraState);
            updateCharacterPreview(windowID, player, camera);
            return;
        }

        PlayerPhotoStruct photo = windowID2Photo.get(windowID);

        if (photo != null) {
            cameraShakeBranch.calm(camera, cameraState);
            updatePhoto(photo, player, camera, raw,
                    windowID2Window.get(windowID).getMenuListHandle().isInputLocked());
            return;
        }

        turnWithDeck(player, camera);

        if (windowID2Window.get(windowID).getMenuListHandle().isInputLocked()) {
            cameraShakeBranch.calm(camera, cameraState);
            return;
        }

        // Translate raw hardware → game intent before anything reads EntityInputHandle
        EntityInputHandle input = player.getEntityInputHandle();
        boolean firstPerson = isFirstPerson(windowID);

        if (windowID2Performing.get(windowID))
            input.clear();
        else
            playerInputSystem.translate(raw, input);

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
        windowID2CharacterPreviewOrbit.put(windowID, 0f);
        windowID2PreviewFocus.remove(windowID);
        windowID2PreviewZoom.put(windowID, 0f);
        windowID2PreviewTurnSeconds.put(windowID, EngineSetting.CHARACTER_PREVIEW_RELEASE_SECONDS);
        windowID2CharacterPreview.put(windowID, true);
    }

    public void endCharacterPreview(int windowID) {

        if (!windowID2CharacterPreview.get(windowID))
            return;

        windowID2CharacterPreview.put(windowID, false);
        windowID2PreviewFraming.remove(windowID);
        windowID2PreviewFocus.remove(windowID);
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

    // The preview's camera turned about the character, which stands still facing the way it did
    public void rotateCharacterPreview(int windowID, float degrees) {

        windowID2CharacterPreviewOrbit.put(
                windowID,
                windowID2CharacterPreviewOrbit.get(windowID) + (float) Math.toRadians(degrees));
        windowID2PreviewTurnSeconds.put(windowID, 0f);
    }

    // The bones the preview closes in on while a slider is worked, or the whole body again for none
    public void focusCharacterPreview(int windowID, int[] bones) {

        if (bones == null || bones.length == 0)
            windowID2PreviewFocus.remove(windowID);
        else
            windowID2PreviewFocus.put(windowID, bones);
    }

    // The way the character faces while previewed
    private Vector3 resolvePreviewFacing(int windowID) {

        float yaw = windowID2CharacterPreviewYaw.get(windowID);

        return facingDirection.set((float) Math.sin(yaw), 0f, (float) Math.cos(yaw));
    }

    // The side of the character the camera stands on: its facing turned by the camera's orbit
    private Vector3 resolvePreviewView(int windowID) {

        float yaw = windowID2CharacterPreviewYaw.get(windowID) + windowID2CharacterPreviewOrbit.get(windowID);

        return viewDirection.set((float) Math.sin(yaw), 0f, (float) Math.cos(yaw));
    }

    // The character stands facing the way it did and the camera looks back at a point beside it from the side the
    // orbit has turned it to; its right axis along -view is (view.z, 0, -view.x)
    private void updateCharacterPreview(int windowID, EntityInstance player, CameraInstance camera) {

        easePreviewZoom(windowID, internal.getDeltaTime());

        Vector3 facing = resolvePreviewFacing(windowID);
        Vector3 view = resolvePreviewView(windowID);
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
            placePreviewCamera(windowID, player, camera, view, placeFramedPreviewCamera(player, camera, view, framing));
            internalBufferSystem.updatePlayerPosition(player.getWorldPositionStruct());
            return;
        }

        Vector3 size = player.getSize();
        float lateral = EngineSetting.CHARACTER_PREVIEW_LATERAL_OFFSET;
        float distance = EngineSetting.CHARACTER_PREVIEW_DISTANCE;
        float lift = EngineSetting.CHARACTER_PREVIEW_LIFT;

        cameraPosition.set(player.getWorldPositionStruct().getPosition());
        cameraPosition.add(
                size.x * 0.5f - view.z * lateral,
                size.y * EngineSetting.CHARACTER_PREVIEW_FOCUS_HEIGHT,
                size.z * 0.5f + view.x * lateral);
        cameraPosition.add(view.x * distance, lift, view.z * distance);

        cameraOffset.set(-view.x * distance, -lift, -view.z * distance);

        placePreviewCamera(windowID, player, camera, view, lateral / distance);
        internalBufferSystem.updatePlayerPosition(player.getWorldPositionStruct());
    }

    // Sets the camera where the preview's placement put it, eased toward the bones a worked slider moves by how far
    // the preview has closed in; the bones sit as far beside the view's centre for their distance as the body does
    private void placePreviewCamera(
            int windowID,
            EntityInstance player,
            CameraInstance camera,
            Vector3 view,
            float lateralShare) {

        int[] bones = windowID2PreviewFocus.get(windowID);
        float zoom = windowID2PreviewZoom.get(windowID);

        if (bones != null && zoom > 0f) {

            float distance = resolveFocus(player, bones) / (float) Math.sin(Math.toRadians(camera.getFOV()) * 0.5);
            float lateral = lateralShare * distance;
            float blend = zoom * zoom * (3f - 2f * zoom);

            focusPoint.set(focusCenter).add(-view.z * lateral, 0f, view.x * lateral);
            focusCamera.set(focusPoint).add(view.x * distance, 0f, view.z * distance);

            cameraOffset.add(cameraPosition);
            cameraOffset.add(
                    (focusPoint.x - cameraOffset.x) * blend,
                    (focusPoint.y - cameraOffset.y) * blend,
                    (focusPoint.z - cameraOffset.z) * blend);
            cameraPosition.add(
                    (focusCamera.x - cameraPosition.x) * blend,
                    (focusCamera.y - cameraPosition.y) * blend,
                    (focusCamera.z - cameraPosition.z) * blend);
            cameraOffset.subtract(cameraPosition);
        }

        camera.setPosition(cameraPosition);
        camera.setDirection(cameraOffset);
    }

    // The middle of the bones as posed, into focusCenter, and the radius that frames them all with the margin
    private float resolveFocus(EntityInstance player, int[] bones) {

        AnimationStateHandle animation = player.getAnimationStateHandle();

        focusCenter.set(0f, 0f, 0f);

        for (int i = 0; i < bones.length; i++) {
            resolveBonePoint(player, animation, bones[i], focusPoint);
            focusCenter.add(focusPoint);
        }

        focusCenter.multiply(1f / bones.length);

        float radius = 0f;

        for (int i = 0; i < bones.length; i++) {
            resolveBonePoint(player, animation, bones[i], focusPoint);
            radius = Math.max(radius, focusPoint.subtract(focusCenter).length());
        }

        return Math.max(
                EngineSetting.CHARACTER_PREVIEW_ZOOM_RADIUS_MIN,
                radius + EngineSetting.CHARACTER_PREVIEW_ZOOM_MARGIN);
    }

    private void resolveBonePoint(EntityInstance player, AnimationStateHandle animation, int bone, Vector3 out) {

        Matrix4 world = animation.getWorldMatrix(bone);

        player.getModelPoint(world.getM03(), world.getM13(), world.getM23(), out);
    }

    // Closes in on a worked slider's bones while the view is still, backs out to the whole body while it turns, and
    // once the turn has stopped turns the view back, the short way round, to where it stood
    private void easePreviewZoom(int windowID, float deltaTime) {

        float seconds = windowID2PreviewTurnSeconds.get(windowID) + deltaTime;
        boolean turning = seconds < EngineSetting.CHARACTER_PREVIEW_RELEASE_SECONDS;
        boolean focused = windowID2PreviewFocus.containsKey(windowID);
        float zoom = windowID2PreviewZoom.get(windowID);
        float target = focused && !turning ? 1f : 0f;

        windowID2PreviewTurnSeconds.put(windowID, seconds);
        windowID2PreviewZoom.put(windowID, zoom + (target - zoom)
                * (1f - (float) Math.exp(-EngineSetting.CHARACTER_PREVIEW_ZOOM_RATE * deltaTime)));

        if (!focused || turning)
            return;

        float orbit = (float) Math.IEEEremainder(windowID2CharacterPreviewOrbit.get(windowID), 2.0 * Math.PI);

        windowID2CharacterPreviewOrbit.put(
                windowID,
                orbit * (float) Math.exp(-EngineSetting.CHARACTER_PREVIEW_RETURN_RATE * deltaTime));
    }

    // Places the camera to frame the body at the screen point and share asked, and gives how far beside the view's
    // centre the body sits for its distance
    private float placeFramedPreviewCamera(
            EntityInstance player,
            CameraInstance camera,
            Vector3 view,
            Vector3 framing) {

        Vector3 size = player.getSize();
        float tanHalfFov = (float) Math.tan(Math.toRadians(camera.getFOV()) * 0.5);
        float aspect = camera.getViewport().x / Math.max(1f, camera.getViewport().y);
        float distance = size.y / (framing.z * 2f * tanHalfFov);
        float lateral = framing.x * distance * tanHalfFov * aspect;
        float vertical = framing.y * distance * tanHalfFov;

        cameraPosition.set(player.getWorldPositionStruct().getPosition());
        cameraPosition.add(
                size.x * 0.5f - view.z * lateral,
                size.y * EngineSetting.CHARACTER_PREVIEW_CENTER_HEIGHT - vertical,
                size.z * 0.5f + view.x * lateral);
        cameraPosition.add(view.x * distance, 0f, view.z * distance);

        cameraOffset.set(-view.x * distance, 0f, -view.z * distance);

        return lateral / distance;
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

    // The being the window's player looks at within a reach, none when it faces no one that near
    public EntityInstance findFacedEntityForWindow(int windowID, float reach) {

        EntityInstance player = windowID2Player.get(windowID);
        CameraInstance camera = windowID2Camera.get(windowID);

        if (player == null || camera == null)
            return null;

        resolveEyePosition(player);

        return combatManager.findTarget(
                player,
                player.getWorldPositionStruct().getChunkCoordinate(),
                eyePosition,
                camera.getDirection(),
                reach);
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

    // True when some window plays as the entity
    public boolean isPlayer(EntityInstance entity) {
        return windowID2Player.containsValue(entity);
    }

    public boolean isFreeCameraForWindow(int windowID) {
        return windowID2FreeCamera.get(windowID);
    }

    // True while the window's player is in play — not flying free, not held, not performing, not posed for a
    // preview or a photo, no menu holding input
    public boolean isPlayingForWindow(int windowID) {

        WindowInstance window = windowID2Window.get(windowID);

        return window != null
                && !windowID2FreeCamera.get(windowID)
                && !windowID2Held.get(windowID)
                && !windowID2Performing.get(windowID)
                && !windowID2CharacterPreview.get(windowID)
                && !windowID2Photo.containsKey(windowID)
                && !window.getMenuListHandle().isInputLocked();
    }

    public CameraInstance getCameraForWindow(int windowID) {
        return windowID2Camera.get(windowID);
    }

    // How much of the mouse's travel turns the window's view — less behind a guard
    public float getLookScaleForWindow(int windowID) {

        EntityInstance player = windowID2Player.get(windowID);

        if (player == null)
            return EngineSetting.LOOK_SCALE_FULL;

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