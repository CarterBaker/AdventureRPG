package application.bootstrap.entitypipeline.playermanager;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.entity.EntityState;
import application.bootstrap.entitypipeline.entity.EntityStateHandle;
import application.bootstrap.entitypipeline.entitymanager.EntityManager;
import application.bootstrap.entitypipeline.placementmanager.PlacementManager;
import application.bootstrap.entitypipeline.util.EntityInputHandle;
import application.bootstrap.physicspipeline.movementmanager.MovementManager;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.chunk.ChunkData;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.util.WorldPositionStruct;
import application.bootstrap.worldpipeline.util.WorldPositionUtility;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import application.kernel.inputpipeline.input.RawInputHandle;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import engine.assets.camera.CameraInstance;
import engine.input.Keys;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class PlayerManager extends ManagerPackage {

    /*
     * Owns and drives the player entity and its camera. spawnPlayer() takes the
     * window the player renders into and the context's RawInputHandle —
     * caller decides both, no internal lookups.
     *
     * The camera trails the player's eye position each frame at an adjustable
     * third-person distance behind it along the view direction. Scroll wheel
     * (raw.getScrollY()) moves the target distance continuously. F5 toggles
     * first person: the first press snapshots whatever third-person distance
     * the scroll wheel had already settled on into
     * windowID2PreFirstPersonZoomTarget and snaps the target straight to
     * EngineSetting.CAMERA_ZOOM_MIN; the second press restores that exact
     * snapshotted distance rather than re-deriving one, so the camera comes
     * back out to precisely where it was before going first person. The
     * actual distance smoothly lerps toward that target every frame rather
     * than snapping, so scroll zoom and the F5 toggle both feel continuous
     * instead of stepping. Gameplay logic (raycasts, placement) always reads
     * from the eye position, never the visual camera position, so aiming
     * stays correct regardless of zoom.
     *
     * isFirstPerson(windowID) exposes whether the current distance is at/below
     * the first-person threshold — consumed by the render side (via
     * EntityRenderSystem.pushCharacter()'s hideHead) to decide whether to
     * hide the character's head, and by movement, which holds the body to
     * the camera in first person by asking the input to strafe. In third
     * person the body turns toward wherever the player heads, so the
     * character runs toward the camera rather than backpedalling. This is a
     * live distance check, not the F5 toggle state directly, so the head
     * fades out/in exactly as the lerp crosses the threshold regardless of
     * whether zero was reached by scrolling or by F5.
     *
     * Camera rotation is driven externally by the runtime context.
     *
     * Only the hovered window's player is updated each frame — WindowManager is
     * the single authority on which window is active. All other players freeze.
     * Movement is additionally gated on the window's menu lock state so that
     * open menus suppress input without any external coordination.
     *
     * The player's animation is advanced here too, right after
     * MovementManager.move() each frame, so its animation tree reads the
     * final movement state, speed, and facing — including the jumping,
     * falling, wading, and swimming states only movement can resolve.
     * EntityInstance.updateAnimation() is the one path that feeds them in;
     * entities with no character model skip it entirely.
     *
     * Character rendering itself — model matrix, entity-size scale, and the
     * actual skinned draw submission — lives entirely in the engine-side
     * EntityRenderSystem.pushCharacter(), shared with every NPC. This class
     * owns only the state that entry point needs: which entity, which
     * camera, and (via isFirstPerson()) which bone to hide.
     *
     * spawnFreeCamera() registers a window exactly like spawnPlayer(), but
     * the entity is only an anchor the world streams around — it is never
     * drawn or animated and never places or breaks blocks. Each frame it is
     * flown through MovementManager.fly() instead of moved, and the camera
     * sits directly at its eye position with no zoom.
     *
     * rerollPlayerForWindow() turns a window's player into a fresh character
     * in place, and verifyPlayerPositionForWindow() holds a player that has
     * just been moved until its new chunk has generated, then settles it on
     * safe ground. A position that is already safe is kept exactly, so a
     * restored save lands where it was left.
     *
     * beginCharacterPreview() turns a window's player into the subject of a
     * character creator: it stands idle, animating, while the camera frames
     * it from the front, offset so the character sits clear of the creator's
     * panels. The preview faces the body by strafing toward the preview
     * direction, so rotateCharacterPreview() turns it smoothly on the spot.
     * endCharacterPreview() hands the camera back looking the way the
     * character faces.
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
    private PlacementManager placementManager;

    // Per-window
    private Int2ObjectOpenHashMap<EntityInstance> windowID2Player;
    private Int2ObjectOpenHashMap<CameraInstance> windowID2Camera;
    private Int2ObjectOpenHashMap<RawInputHandle> windowID2RawInput;
    private Int2ObjectOpenHashMap<WindowInstance> windowID2Window;
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

    // Scratch
    private Vector3 cameraPosition;
    private Vector3 cameraOffset;
    private Vector3 eyePosition;
    private Vector3 facingDirection;

    // Internal \\

    @Override
    protected void create() {

        this.playerInputSystem = create(PlayerInputSystem.class);
        this.internalBufferSystem = create(PlayerBufferSystem.class);
        this.placementManager = create(PlacementManager.class);

        this.windowID2Player = new Int2ObjectOpenHashMap<>();
        this.windowID2Camera = new Int2ObjectOpenHashMap<>();
        this.windowID2RawInput = new Int2ObjectOpenHashMap<>();
        this.windowID2Window = new Int2ObjectOpenHashMap<>();
        this.windowID2VerifyPlayerPosition = new Int2BooleanOpenHashMap();
        this.windowID2FreeCamera = new Int2BooleanOpenHashMap();

        this.windowID2ZoomDistance = new Int2FloatOpenHashMap();
        this.windowID2ZoomTarget = new Int2FloatOpenHashMap();

        this.windowID2FirstPersonToggled = new Int2BooleanOpenHashMap();
        this.windowID2PreFirstPersonZoomTarget = new Int2FloatOpenHashMap();

        this.windowID2CharacterPreview = new Int2BooleanOpenHashMap();
        this.windowID2CharacterPreviewYaw = new Int2FloatOpenHashMap();

        this.cameraPosition = new Vector3();
        this.cameraOffset = new Vector3();
        this.eyePosition = new Vector3();
        this.facingDirection = new Vector3();
    }

    @Override
    protected void get() {
        this.movementManager = get(MovementManager.class);
        this.entityManager = get(EntityManager.class);
        this.blockManager = get(BlockManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
        this.windowManager = get(WindowManager.class);
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
        return registerPlayer(window, rawInput, false);
    }

    public EntityInstance spawnFreeCamera(WindowInstance window, RawInputHandle rawInput) {
        return registerPlayer(window, rawInput, true);
    }

    private EntityInstance registerPlayer(WindowInstance window, RawInputHandle rawInput, boolean freeCamera) {
        EntityInstance player = entityManager.spawnEntity(EngineSetting.DEFAULT_PLAYER_RACE);
        int windowID = window.getWindowID();
        windowID2Player.put(windowID, player);
        windowID2Camera.put(windowID, window.getActiveCamera());
        windowID2RawInput.put(windowID, rawInput);
        windowID2Window.put(windowID, window);
        windowID2VerifyPlayerPosition.put(windowID, true);
        windowID2FreeCamera.put(windowID, freeCamera);
        windowID2ZoomDistance.put(windowID, EngineSetting.CAMERA_ZOOM_DEFAULT);
        windowID2ZoomTarget.put(windowID, EngineSetting.CAMERA_ZOOM_DEFAULT);
        windowID2FirstPersonToggled.put(windowID, false);
        windowID2PreFirstPersonZoomTarget.put(windowID, EngineSetting.CAMERA_ZOOM_DEFAULT);
        windowID2CharacterPreview.put(windowID, false);
        windowID2CharacterPreviewYaw.put(windowID, 0f);
        return player;
    }

    // Character \\

    public void rerollPlayerForWindow(int windowID) {
        entityManager.rerollEntity(windowID2Player.get(windowID));
        verifyPlayerPositionForWindow(windowID);
    }

    public void verifyPlayerPositionForWindow(int windowID) {
        windowID2VerifyPlayerPosition.put(windowID, true);
    }

    // Player \\

    private void calculatePlayerPosition(
            int windowID,
            EntityInstance player,
            CameraInstance camera,
            RawInputHandle raw) {

        WorldPositionStruct worldPositionStruct = player.getWorldPositionStruct();
        boolean verifyPlayerPosition = windowID2VerifyPlayerPosition.get(windowID);

        if (verifyPlayerPosition) {
            verifyPlayerPosition = verifyPlayerPosition(player, worldPositionStruct);
            windowID2VerifyPlayerPosition.put(windowID, verifyPlayerPosition);
            return;
        }

        if (windowID2CharacterPreview.get(windowID)) {
            updateCharacterPreview(windowID, player, camera);
            return;
        }

        if (windowID2Window.get(windowID).getMenuListHandle().isInputLocked())
            return;

        // Translate raw hardware → game intent before anything reads EntityInputHandle
        playerInputSystem.translate(raw, player.getEntityInputHandle());
        player.getEntityInputHandle().setStrafe(isFirstPerson(windowID));

        if (windowID2FreeCamera.get(windowID)) {
            calculateFreeCameraPosition(player, camera);
            return;
        }

        writeMovementState(player);
        movementManager.move(player);
        player.updateAnimation(internal.getDeltaTime());

        // Eye position — where gameplay (aiming, raycasts) actually happens,
        // regardless of where the visual camera ends up.
        resolveEyePosition(player);

        float distance = updateZoom(windowID, raw);

        // Visual camera — pulled back behind the eye along the view direction.
        // distance == 0 puts it exactly at the eye (first person).
        Vector3 direction = camera.getDirection();

        cameraPosition.set(eyePosition);
        cameraPosition.subtract(
                direction.x * distance,
                direction.y * distance,
                direction.z * distance);

        camera.setPosition(cameraPosition);

        EntityInputHandle input = player.getEntityInputHandle();
        placementManager.update(
                player,
                eyePosition,
                camera.getDirection(),
                input.isPrimaryAction(),
                input.isSecondaryAction());

        internalBufferSystem.updatePlayerPosition(worldPositionStruct);
    }

    private void calculateFreeCameraPosition(EntityInstance anchor, CameraInstance camera) {

        movementManager.fly(anchor);

        resolveEyePosition(anchor);
        camera.setPosition(eyePosition);

        internalBufferSystem.updatePlayerPosition(anchor.getWorldPositionStruct());
    }

    private void resolveEyePosition(EntityInstance player) {

        cameraOffset.set(
                player.getSize().x / 2,
                player.getEyeHeight(),
                player.getSize().z / 2);

        eyePosition.set(player.getWorldPositionStruct().getPosition());
        eyePosition.add(cameraOffset);
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
        windowID2Camera.get(windowID).setDirection(resolvePreviewFacing(windowID));
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

        movementManager.face(player);
        player.updateAnimation(internal.getDeltaTime());

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

    // Zoom \\

    /*
     * Scroll adjusts the target distance continuously; F5 toggles first
     * person via toggleFirstPerson() below. The actual distance smoothly
     * lerps toward whatever the target is every frame — this is what
     * produces the "camera eases toward the new distance" feel rather than
     * an instant jump, and also means scroll and F5 read/write the same
     * target rather than fighting over two different values.
     */
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

    /*
     * First press: snapshots whatever third-person target distance the
     * scroll wheel had already settled on for this window, marks this
     * window as toggled into first person, and returns
     * EngineSetting.CAMERA_ZOOM_MIN as the new target.
     *
     * Second press: clears the toggle and returns the snapshotted distance,
     * so the camera target jumps straight back to (and then smoothly lerps
     * toward, same as any other target change) exactly the third-person
     * distance it was at before the first press — never re-derived from a
     * fixed default.
     */
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

    private void writeMovementState(EntityInstance player) {

        EntityStateHandle state = player.getEntityStateHandle();
        EntityInputHandle input = player.getEntityInputHandle();

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

    public CameraInstance getCameraForWindow(int windowID) {
        return windowID2Camera.get(windowID);
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