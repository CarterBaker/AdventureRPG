package editor.worldmap.view;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.mappipeline.map.MapViewStruct;
import application.bootstrap.mappipeline.mapmanager.MapManager;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.util.WorldPositionStruct;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import application.kernel.inputpipeline.input.RawInputHandle;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.runtime.EditorInputSystem;
import editor.worldmap.WorldMapSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WorldMapViewSystem extends SystemPackage {

    /*
     * Where the world map looks and what it follows. The map shows the world
     * of the first streaming grid with a focal entity, north up, and tracks
     * that character's position and facing every frame. Scrolling zooms about
     * the pointer, from a few pixels per block out to the whole world, and
     * dragging pans; panning lets go of the character, and following snaps
     * the view back onto it. The settled view is handed to the engine's map
     * each frame, which answers with the tiles to draw. Positions are world
     * blocks, wrapped like the world itself, and screen positions are window
     * pixels with y up.
     */

    // Internal
    private EditorInputSystem editorInputSystem;
    private WorldStreamManager worldStreamManager;
    private PlayerManager playerManager;
    private MapManager mapManager;

    // World
    private GridInstance grid;
    private WorldHandle worldHandle;

    // View
    private MapViewStruct mapView;
    private double centerX;
    private double centerZ;
    private double blocksPerPixel;
    private boolean following;

    // Pan
    private boolean panning;
    private float lastMouseX;
    private float lastMouseY;

    // Player
    private boolean hasPlayer;
    private double playerX;
    private double playerZ;
    private float headingX;
    private float headingZ;

    // Base \\

    @Override
    protected void create() {
        this.mapView = new MapViewStruct();
        this.blocksPerPixel = WorldMapSetting.DEFAULT_BLOCKS_PER_PIXEL;
        this.following = true;
        this.headingZ = -1f;
    }

    @Override
    protected void get() {
        this.editorInputSystem = get(EditorInputSystem.class);
        this.worldStreamManager = get(WorldStreamManager.class);
        this.playerManager = get(PlayerManager.class);
        this.mapManager = get(MapManager.class);
    }

    // Update \\

    @Override
    protected void update() {

        resolveGrid();
        resolvePlayer();

        if (worldHandle == null)
            return;

        syncMapView();
        handleZoom();
        handlePan();

        if (following && hasPlayer) {
            centerX = playerX;
            centerZ = playerZ;
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

        worldHandle = grid != null ? grid.getWorldHandle() : null;
    }

    private void resolvePlayer() {

        hasPlayer = grid != null;

        if (!hasPlayer)
            return;

        EntityInstance focalEntity = grid.getFocalEntity();
        WorldPositionStruct position = focalEntity.getWorldPositionStruct();
        long chunkCoordinate = position.getChunkCoordinate();
        Vector3 local = position.getPosition();

        playerX = (double) Coordinate2Long.unpackX(chunkCoordinate) * EngineSetting.CHUNK_SIZE + local.x;
        playerZ = (double) Coordinate2Long.unpackY(chunkCoordinate) * EngineSetting.CHUNK_SIZE + local.z;

        WindowInstance gridWindow = grid.getWindowInstance();

        if (gridWindow == null || !playerManager.hasPlayerForWindow(gridWindow.getWindowID()))
            return;

        Vector3 direction = playerManager.getCameraForWindow(gridWindow.getWindowID()).getDirection();
        float length = (float) Math.sqrt(direction.x * direction.x + direction.z * direction.z);

        if (length <= 0f)
            return;

        headingX = direction.x / length;
        headingZ = direction.z / length;
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

        if (editorInputSystem.isClicked(WorldMapSetting.BUTTON_PAN))
            panning = true;

        if (!rawInput.isButtonHeld(WorldMapSetting.BUTTON_PAN))
            panning = false;

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

    public boolean hasWorld() {
        return worldHandle != null;
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

    public boolean hasPlayer() {
        return hasPlayer;
    }

    public double getPlayerX() {
        return playerX;
    }

    public double getPlayerZ() {
        return playerZ;
    }

    public float getHeadingX() {
        return headingX;
    }

    public float getHeadingZ() {
        return headingZ;
    }
}
