package editor.worldmap.view;

import application.bootstrap.mappipeline.map.MapMarkerStruct;
import application.bootstrap.mappipeline.map.MapViewStruct;
import application.bootstrap.mappipeline.mapmanager.MapManager;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import application.kernel.inputpipeline.input.RawInputHandle;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.bootstrap.worldeditorpipeline.worldeditormanager.WorldEditorManager;
import editor.runtime.EditorInputSystem;
import editor.worldmap.WorldMapSetting;
import engine.root.SystemPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WorldMapViewSystem extends SystemPackage {

    /*
     * Where the world map looks and what it follows. The map shows the world
     * of the first streaming grid with a focal entity, or the active world
     * while no preview is open, north up, and tracks that character's
     * position and facing every frame through the engine's map markers. It
     * also holds which shared overlays, day and night and weather, this map
     * shows. Scrolling zooms about the pointer, from a few pixels per block
     * out to the whole world, and dragging pans; panning lets go of the
     * character, and following snaps the view back onto it. A click that
     * does not drag selects the biome under the pointer in the hierarchy. The
     * settled view is handed to the engine's map each frame, which answers
     * with the tiles to draw. Positions are world blocks, wrapped like the
     * world itself, and screen positions are window pixels with y up.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private WorldStreamManager worldStreamManager;
    private WorldManager worldManager;
    private MapManager mapManager;
    private WorldEditorManager worldEditorManager;

    // World
    private GridInstance grid;
    private WorldHandle worldHandle;

    // View
    private MapViewStruct mapView;
    private double centerX;
    private double centerZ;
    private double blocksPerPixel;
    private boolean following;

    // Overlays
    private boolean showingDayNight;
    private boolean showingWeather;

    // Pan
    private boolean panning;
    private float lastMouseX;
    private float lastMouseY;
    private float pressMouseX;
    private float pressMouseY;

    // Player
    private boolean hasPlayer;
    private MapMarkerStruct playerMarker;

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
        this.worldStreamManager = get(WorldStreamManager.class);
        this.worldManager = get(WorldManager.class);
        this.mapManager = get(MapManager.class);
        this.worldEditorManager = get(WorldEditorManager.class);
    }

    // Update \\

    @Override
    protected void update() {

        resolveGrid();
        resolvePlayer();
        syncMapView();
        handleZoom();
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
        mapView.setOverlays(showingDayNight, showingWeather);
    }

    // World \\

    private void resolveGrid() {

        ObjectArrayList<GridInstance> grids = worldStreamManager.getGrids();
        Object[] elements = grids.elements();
        int size = grids.size();

        grid = null;

        for (int i = 0; i < size; i++) {

            GridInstance candidate = (GridInstance) elements[i];

            if (candidate.getFocalEntity() != null && candidate.getWorldHandle() != null) {
                grid = candidate;
                break;
            }
        }

        worldHandle = grid != null ? grid.getWorldHandle() : worldManager.getActiveWorld();
    }

    private void resolvePlayer() {

        hasPlayer = grid != null;

        if (hasPlayer)
            mapManager.resolveMarker(grid, playerMarker);
    }

    // Input \\

    private void handleZoom() {

        if (!editorInputSystem.isPointerActive())
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

    private void handlePan() {

        RawInputHandle rawInput = editorInputSystem.getRawInputHandle();
        float mouseX = rawInput.getMouseX();
        float mouseY = rawInput.getMouseY();

        if (editorInputSystem.isClicked(WorldMapSetting.BUTTON_PAN)) {
            panning = true;
            pressMouseX = mouseX;
            pressMouseY = mouseY;
        }

        if (panning && !rawInput.isButtonHeld(WorldMapSetting.BUTTON_PAN)) {

            panning = false;

            if (Math.abs(mouseX - pressMouseX) <= WorldMapSetting.CLICK_SLOP_PIXELS
                    && Math.abs(mouseY - pressMouseY) <= WorldMapSetting.CLICK_SLOP_PIXELS)
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

    private double clampZoom(double zoom) {

        WindowInstance window = context.getWindow();
        double maxBlocksPerPixel = Math.max(
                worldHandle.getWorldScale().x / (double) Math.max(window.getWidth(), 1),
                worldHandle.getWorldScale().y / (double) Math.max(window.getHeight(), 1));

        return Math.max(WorldMapSetting.MIN_BLOCKS_PER_PIXEL, Math.min(maxBlocksPerPixel, zoom));
    }

    // Management \\

    public void toggleFollow() {
        following = !following;
    }

    public void toggleDayNight() {
        showingDayNight = !showingDayNight;
    }

    public void toggleWeather() {
        showingWeather = !showingWeather;
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
        return showingDayNight;
    }

    public boolean isShowingWeather() {
        return showingWeather;
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

    public float getHeadingX() {
        return playerMarker.getHeadingX();
    }

    public float getHeadingZ() {
        return playerMarker.getHeadingZ();
    }
}
