package editor.runtime.worldmap.view;

import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.mappipeline.map.MapMarkerStruct;
import application.bootstrap.mappipeline.map.MapViewStruct;
import application.bootstrap.mappipeline.mapmanager.MapManager;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.util.GlobeUtility;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.GlobeTexelStruct;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import application.kernel.inputpipeline.input.RawInputHandle;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.worldeditorpipeline.worldeditormanager.WorldEditorManager;
import editor.runtime.input.EditorInputSystem;
import editor.runtime.viewcamera.ViewCameraSubject;
import editor.runtime.viewcamera.ViewCameraSystem;
import editor.runtime.worldmap.WorldMapSetting;
import editor.runtime.worldmap.layer.WorldMapLayer;
import editor.runtime.worldmap.layer.WorldMapLayerSystem;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.vectors.Vector3;
import engine.util.mathematics.vectors.Vector3Double;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WorldMapViewSystem extends SystemPackage implements ViewCameraSubject {

    /*
     * Where the world map looks and what it follows. The map shows the
     * active world and only it, never a world of the editor's own such as
     * the biome planner's, north up, and tracks the character of the first
     * streaming grid standing in it, its position and facing every frame
     * through the engine's map markers. The
     * shared overlays, day and night and weather, and the character's arrow
     * show as the map's layers say. Scrolling zooms about the pointer, from a few pixels per block
     * out to the whole world, and dragging pans; panning lets go of the
     * character, and following snaps the view back onto it. A press on the
     * character's arrow, while shown, picks it up instead, and dropping it anywhere else
     * moves that window's character to the block column under it. A click
     * that does not drag selects the biome under the pointer in the
     * hierarchy. The settled view is handed to the engine's map each frame,
     * which answers with the tiles to draw. Positions are world blocks,
     * wrapped like the world itself, and screen positions are window pixels
     * with y up. A globe world is shown on its globe instead, a ball of unit
     * radius the shared view camera orbits and closes on from its surface;
     * following turns the view to look straight down on the character,
     * orbiting lets go of it, a press is read where its pick ray strikes the
     * ball, at the world's own position there, and the view's centre is the
     * ground under the window's middle.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private WorldStreamManager worldStreamManager;
    private WorldManager worldManager;
    private MapManager mapManager;
    private WorldEditorManager worldEditorManager;
    private PlayerManager playerManager;

    // World
    private GridInstance grid;
    private WorldHandle worldHandle;

    // View
    private MapViewStruct mapView;
    private double centerX;
    private double centerZ;
    private double blocksPerPixel;
    private boolean following;

    // Layers
    private WorldMapLayerSystem worldMapLayerSystem;

    // Pan
    private boolean panning;
    private float lastMouseX;
    private float lastMouseY;
    private float pressMouseX;
    private float pressMouseY;

    // Marker Drag
    private boolean draggingMarker;
    private double dragX;
    private double dragZ;

    // Player
    private boolean hasPlayer;
    private MapMarkerStruct playerMarker;

    // Globe
    private ViewCameraSystem viewCameraSystem;
    private final Vector3Double globeDirection = new Vector3Double();
    private final GlobeTexelStruct blockScratch = new GlobeTexelStruct();
    private final Vector3 aimScratch = new Vector3();
    private boolean globePressed;
    private final Vector3Double dragPosition = new Vector3Double();
    private final Vector3Double pointerPosition = new Vector3Double();

    // Base \\

    @Override
    protected void create() {
        this.mapView = new MapViewStruct();
        this.blocksPerPixel = WorldMapSetting.DEFAULT_BLOCKS_PER_PIXEL;
        this.following = true;
        this.playerMarker = new MapMarkerStruct();
    }

    @Override
    protected void get() {
        this.editorInputSystem = get(EditorInputSystem.class);
        this.worldMapLayerSystem = get(WorldMapLayerSystem.class);
        this.worldStreamManager = get(WorldStreamManager.class);
        this.worldManager = get(WorldManager.class);
        this.mapManager = get(MapManager.class);
        this.worldEditorManager = get(WorldEditorManager.class);
        this.playerManager = get(PlayerManager.class);
        this.viewCameraSystem = get(ViewCameraSystem.class);
    }

    // Update \\

    @Override
    protected void update() {

        resolveGrid();
        resolvePlayer();

        if (isShown()) {
            updateGlobe();
            return;
        }

        syncMapView();
        handleZoom();
        handlePress();
        handleMarkerDrag();
        handlePan();

        if (following && hasPlayer) {
            centerX = playerMarker.getX();
            centerZ = playerMarker.getZ();
        }

        centerX = WorldWrapUtility.wrapBlockX(worldHandle, centerX);
        centerZ = WorldWrapUtility.wrapBlockZ(worldHandle, centerZ);
        blocksPerPixel = clampZoom(blocksPerPixel);

        syncMapView();

        WindowInstance window = context.getWindow();

        if (window.getWidth() > 0 && window.getHeight() > 0)
            mapManager.resolveView(mapView);
    }

    private void syncMapView() {

        WindowInstance window = context.getWindow();

        mapView.set(worldHandle, centerX, centerZ, blocksPerPixel, window.getWidth(), window.getHeight());
        mapView.setOverlays(isShowingDayNight(), isShowingWeather());
    }

    // Globe \\

    // The globe seen through the window's camera, followed, pressed and handed to the engine's map
    private void updateGlobe() {

        WindowInstance window = context.getWindow();

        if (viewCameraSystem.isDragging())
            following = false;

        if (following && hasPlayer)
            aimAtPlayer();

        syncMapView();
        mapView.getGlobeView().resolveCamera(window.getActiveCamera());

        if (strikeWorld(window.getWidth() * 0.5f, window.getHeight() * 0.5f, dragPosition)) {
            centerX = dragPosition.x;
            centerZ = dragPosition.z;
        }

        handleGlobePress();
        handleGlobeMarkerDrag();

        if (window.getWidth() > 0 && window.getHeight() > 0)
            mapManager.resolveView(mapView);
    }

    // The view turned to look straight down on the character, the point it turns about the globe's middle
    private void aimAtPlayer() {

        GlobeUtility.resolveDirection(worldHandle, playerMarker.getX(), playerMarker.getZ(), globeDirection);

        aimScratch.set((float) -globeDirection.x, (float) -globeDirection.y, (float) -globeDirection.z);
        viewCameraSystem.getViewCameraState().aim(aimScratch);
        viewCameraSystem.getViewCameraState().getPan().set(0f, 0f, 0f);
    }

    // A press on the character's arrow picks it up and is the map's own; any other is the camera's, and one let go
    // where it was pressed selects the biome struck
    private void handleGlobePress() {

        RawInputHandle rawInput = editorInputSystem.getRawInputHandle();
        float mouseX = rawInput.getMouseX();
        float mouseY = rawInput.getMouseY();

        if (editorInputSystem.isClicked(WorldMapSetting.BUTTON_PAN)) {

            pressMouseX = mouseX;
            pressMouseY = mouseY;

            if (isOverGlobeMarker(mouseX, mouseY)) {
                editorInputSystem.claim(WorldMapSetting.BUTTON_PAN);
                draggingMarker = true;
            } else
                globePressed = true;
        }

        if (!globePressed || rawInput.isButtonHeld(WorldMapSetting.BUTTON_PAN))
            return;

        globePressed = false;

        if (isClick(mouseX, mouseY) && strikeWorld(mouseX, mouseY, dragPosition))
            worldEditorManager.selectBiomeAt(worldHandle, dragPosition.x, dragPosition.z);
    }

    private void handleGlobeMarkerDrag() {

        if (!draggingMarker)
            return;

        RawInputHandle rawInput = editorInputSystem.getRawInputHandle();
        float mouseX = rawInput.getMouseX();
        float mouseY = rawInput.getMouseY();

        if (strikeWorld(mouseX, mouseY, dragPosition)) {
            this.dragX = dragPosition.x;
            this.dragZ = dragPosition.z;
        }

        if (rawInput.isButtonHeld(WorldMapSetting.BUTTON_PAN))
            return;

        draggingMarker = false;

        if (!isClick(mouseX, mouseY))
            dropMarker();
    }

    // The world's own block position a window position strikes on the ball, its x and z left in out
    private boolean strikeWorld(float screenX, float screenY, Vector3Double out) {

        WindowInstance window = context.getWindow();

        if (!mapView.getGlobeView().strike(
                window.getActiveCamera(),
                screenX / window.getWidth() * 2f - 1f,
                screenY / window.getHeight() * 2f - 1f,
                globeDirection))
            return false;

        GlobeUtility.locateBlock(worldHandle, globeDirection.x, globeDirection.y, globeDirection.z, blockScratch);

        out.set(
                GlobeUtility.faceOriginX(blockScratch.getFace(), GlobeUtility.getFaceBlocks(worldHandle))
                        + blockScratch.getX(),
                0.0,
                blockScratch.getY());

        return true;
    }

    private boolean isOverGlobeMarker(float screenX, float screenY) {

        WindowInstance window = context.getWindow();

        if (!isShowingPlayer())
            return false;

        GlobeUtility.resolveDirection(worldHandle, playerMarker.getX(), playerMarker.getZ(), globeDirection);

        if (!mapView.getGlobeView().projectSurface(
                globeDirection.x, globeDirection.y, globeDirection.z, window.getWidth(), window.getHeight()))
            return false;

        float deltaX = screenX - mapView.getGlobeView().getProjectedX();
        float deltaY = screenY - mapView.getGlobeView().getProjectedY();
        float radius = WorldMapSetting.MARKER_GRAB_RADIUS_PIXELS;

        return deltaX * deltaX + deltaY * deltaY <= radius * radius;
    }

    // Pointer \\

    // The world's own block position under a window position, false on a globe where the pointer misses the ball
    public boolean locatePointer(float screenX, float screenY) {

        if (isShown())
            return strikeWorld(screenX, screenY, pointerPosition);

        pointerPosition.set(screenToWorldX(screenX), 0.0, screenToWorldZ(screenY));

        return true;
    }

    public double getPointerX() {
        return pointerPosition.x;
    }

    public double getPointerZ() {
        return pointerPosition.z;
    }

    // The latitude and longitude of the place last struck on the globe, in degrees
    public double getPointerLatitude() {
        return GlobeUtility.resolveLatitudeFactor(globeDirection) * EngineSetting.QUARTER_TURN_DEGREES;
    }

    public double getPointerLongitude() {
        return GlobeUtility.resolveLongitudeFraction(globeDirection) * EngineSetting.DEGREES_PER_FULL_ROTATION;
    }

    // View Camera Subject \\

    // The globe the view camera orbits: only a globe world is shown on one
    @Override
    public boolean isShown() {
        return worldHandle != null && worldHandle.isGlobe();
    }

    @Override
    public float getCenter(int axis) {
        return 0f;
    }

    @Override
    public float getBoundingRadius() {
        return WorldMapSetting.GLOBE_RADIUS;
    }

    @Override
    public float getSurfaceRadius() {
        return WorldMapSetting.GLOBE_RADIUS;
    }

    // World \\

    private void resolveGrid() {

        ObjectArrayList<GridInstance> grids = worldStreamManager.getGrids();
        Object[] elements = grids.elements();
        int size = grids.size();

        worldHandle = worldManager.getActiveWorld();
        grid = null;

        for (int i = 0; i < size; i++) {

            GridInstance candidate = (GridInstance) elements[i];

            if (candidate.getFocalEntity() != null && candidate.getWorldHandle() == worldHandle) {
                grid = candidate;
                break;
            }
        }
    }

    private void resolvePlayer() {

        hasPlayer = grid != null;

        if (hasPlayer)
            mapManager.resolveMarker(grid, playerMarker);
    }

    // Input \\

    private void handleZoom() {

        if (!editorInputSystem.isPointerOver())
            return;

        RawInputHandle rawInput = editorInputSystem.getRawInputHandle();
        float scroll = rawInput.getScrollY();

        if (scroll == 0f)
            return;

        double zoomed = clampZoom(blocksPerPixel * Math.pow(WorldMapSetting.ZOOM_STEP, -scroll));

        // A free view keeps the block under the pointer in place; a followed view zooms about the character
        if (!following) {
            double pointerX = screenToWorldX(rawInput.getMouseX());
            double pointerZ = screenToWorldZ(rawInput.getMouseY());
            centerX = pointerX + (centerX - pointerX) * zoomed / blocksPerPixel;
            centerZ = pointerZ + (centerZ - pointerZ) * zoomed / blocksPerPixel;
        }

        blocksPerPixel = zoomed;
    }

    // A press on the character's arrow picks it up; anywhere else it starts a pan
    private void handlePress() {

        if (!editorInputSystem.isClicked(WorldMapSetting.BUTTON_PAN))
            return;

        RawInputHandle rawInput = editorInputSystem.getRawInputHandle();

        pressMouseX = rawInput.getMouseX();
        pressMouseY = rawInput.getMouseY();

        if (isOverMarker(pressMouseX, pressMouseY))
            draggingMarker = true;
        else
            panning = true;
    }

    private void handleMarkerDrag() {

        if (!draggingMarker)
            return;

        RawInputHandle rawInput = editorInputSystem.getRawInputHandle();
        float mouseX = rawInput.getMouseX();
        float mouseY = rawInput.getMouseY();

        dragX = WorldWrapUtility.wrapBlockX(worldHandle, screenToWorldX(mouseX));
        dragZ = WorldWrapUtility.wrapBlockZ(worldHandle, screenToWorldZ(mouseY));

        if (rawInput.isButtonHeld(WorldMapSetting.BUTTON_PAN))
            return;

        draggingMarker = false;

        if (!isClick(mouseX, mouseY))
            dropMarker();
    }

    private void handlePan() {

        RawInputHandle rawInput = editorInputSystem.getRawInputHandle();
        float mouseX = rawInput.getMouseX();
        float mouseY = rawInput.getMouseY();

        if (panning && !rawInput.isButtonHeld(WorldMapSetting.BUTTON_PAN)) {

            panning = false;

            if (isClick(mouseX, mouseY))
                worldEditorManager.selectBiomeAt(worldHandle, screenToWorldX(mouseX), screenToWorldZ(mouseY));
        }

        if (panning && (mouseX != lastMouseX || mouseY != lastMouseY)) {
            following = false;
            centerX -= (mouseX - lastMouseX) * blocksPerPixel;
            centerZ += (mouseY - lastMouseY) * blocksPerPixel;
        }

        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }

    private boolean isClick(float mouseX, float mouseY) {
        return Math.abs(mouseX - pressMouseX) <= WorldMapSetting.CLICK_SLOP_PIXELS
                && Math.abs(mouseY - pressMouseY) <= WorldMapSetting.CLICK_SLOP_PIXELS;
    }

    private double clampZoom(double zoom) {

        WindowInstance window = context.getWindow();
        double maxBlocksPerPixel = Math.max(
                worldHandle.getWorldScale().x / (double) Math.max(window.getWidth(), 1),
                worldHandle.getWorldScale().y / (double) Math.max(window.getHeight(), 1));

        return Math.max(WorldMapSetting.MIN_BLOCKS_PER_PIXEL, Math.min(maxBlocksPerPixel, zoom));
    }

    // Marker \\

    // Whether a screen point lies on the character's arrow, at the copy of its position nearest the view's centre
    private boolean isOverMarker(float screenX, float screenY) {

        if (!isShowingPlayer())
            return false;

        float deltaX = screenX - mapView.worldToScreenX(centerX + WorldWrapUtility.wrappedDelta(
                playerMarker.getX(), centerX, worldHandle.getWorldScale().x));
        float deltaY = screenY - mapView.worldToScreenY(centerZ + WorldWrapUtility.wrappedDelta(
                playerMarker.getZ(), centerZ, worldHandle.getWorldScale().y));
        float radius = WorldMapSetting.MARKER_GRAB_RADIUS_PIXELS;

        return deltaX * deltaX + deltaY * deltaY <= radius * radius;
    }

    // The character of the window the followed grid streams for moves to the block column the arrow was dropped on
    private void dropMarker() {

        WindowInstance gridWindow = grid != null ? grid.getWindowInstance() : null;

        if (gridWindow == null)
            return;

        long blockX = (long) Math.floor(dragX);
        long blockZ = (long) Math.floor(dragZ);

        playerManager.teleportPlayerForWindow(
                gridWindow.getWindowID(),
                Coordinate2Long.pack(
                        (int) Math.floorDiv(blockX, EngineSetting.CHUNK_SIZE),
                        (int) Math.floorDiv(blockZ, EngineSetting.CHUNK_SIZE)),
                (int) Math.floorMod(blockX, EngineSetting.CHUNK_SIZE),
                (int) Math.floorMod(blockZ, EngineSetting.CHUNK_SIZE));
    }

    // Management \\

    public void toggleFollow() {
        following = !following;
    }

    // Conversion — window pixels, y up, to world blocks and back \\

    public double screenToWorldX(float screenX) {
        return mapView.screenToWorldX(screenX);
    }

    public double screenToWorldZ(float screenY) {
        return mapView.screenToWorldZ(screenY);
    }

    // Accessible \\

    public MapViewStruct getMapView() {
        return mapView;
    }

    public GridInstance getGrid() {
        return grid;
    }

    public WorldHandle getWorldHandle() {
        return worldHandle;
    }

    public double getCenterX() {
        return centerX;
    }

    public double getCenterZ() {
        return centerZ;
    }

    public double getBlocksPerPixel() {
        return blocksPerPixel;
    }

    public boolean isFollowing() {
        return following;
    }

    public boolean isShowingDayNight() {
        return worldMapLayerSystem.isShown(WorldMapLayer.DAY_NIGHT);
    }

    public boolean isShowingWeather() {
        return worldMapLayerSystem.isShown(WorldMapLayer.WEATHER);
    }

    // The character's arrow, drawn and grabbed only while its layer is shown
    public boolean isShowingPlayer() {
        return hasPlayer && worldMapLayerSystem.isShown(WorldMapLayer.PLAYER);
    }

    public boolean hasPlayer() {
        return hasPlayer;
    }

    public double getPlayerX() {
        return playerMarker.getX();
    }

    public double getPlayerZ() {
        return playerMarker.getZ();
    }

    // Where the arrow is drawn: under the pointer while it is carried, on the character otherwise
    public double getMarkerX() {
        return draggingMarker ? dragX : playerMarker.getX();
    }

    public double getMarkerZ() {
        return draggingMarker ? dragZ : playerMarker.getZ();
    }

    public boolean isDraggingMarker() {
        return draggingMarker;
    }

    public float getHeadingX() {
        return playerMarker.getHeadingX();
    }

    public float getHeadingZ() {
        return playerMarker.getHeadingZ();
    }
}
