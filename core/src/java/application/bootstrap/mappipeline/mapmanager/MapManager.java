package application.bootstrap.mappipeline.mapmanager;

import java.nio.ByteBuffer;

import application.bootstrap.mappipeline.map.MapDrawStruct;
import application.bootstrap.mappipeline.map.MapMarkerStruct;
import application.bootstrap.mappipeline.map.MapTileInstance;
import application.bootstrap.mappipeline.map.MapViewStruct;
import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import application.bootstrap.worldpipeline.biomemanager.BiomeManager;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.layout.LayoutPlanStruct;
import application.bootstrap.worldpipeline.util.GlobeUtility;
import application.bootstrap.worldpipeline.util.GlobeViewStruct;
import application.bootstrap.worldpipeline.world.GlobeCapStruct;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.vectors.Vector3;
import engine.util.mathematics.vectors.Vector3Double;
import engine.util.memory.BufferUtility;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

public class MapManager extends ManagerPackage {

    /*
     * Owns the world map: a pyramid of tiles generated on the fly and shared
     * by every view that shows the world from above. Level zero covers the
     * world in a few tiles, every level below splits each tile into four, and
     * the finest shows one block per texel, one macro tile per tile.
     * resolveView() picks the level whose texels best match a view's zoom,
     * asks for the tiles it shows, and stands a coarser tile already drawn in
     * for any tile still generating, so detail sharpens in place as tiles
     * arrive. Tiles generate on the WorldMap pool, upload under a per-frame
     * budget, have the real chunks under them drawn in once a view shows them
     * at the finest level, and are recycled with their textures once no view
     * has shown them for longest; the level zero tiles are always kept, so the
     * whole world shows at once. Tile indices wrap with the world. Live edits
     * mark the tiles they reach stale — an edited region of the world image,
     * or every tile once a biome is rebuilt — and a stale tile keeps showing
     * until its fresh pixels upload, so edits sharpen in place. Views may also
     * show the shared day and night and weather overlays MapOverlayBranch
     * keeps over their tiles, and any map can ask where the players stand.
     * The editor may hand over one preview layout, drawn on every tile it
     * reaches as if it stood but never laid into the world; changing it marks
     * the tiles under the old and the new one stale.
     * On a globe the pyramid stands on the block cube's six faces, level
     * zero a face to a tile, so every tile is a square of one face; a view
     * refines each face's tiles until a texel shows no wider than a pixel,
     * keeping only those the camera sees above the horizon and inside the
     * window, and hands each over with the window rectangle it shows in and
     * its square of its face, which the globe's shader keeps it to. Edits
     * reach the tiles within the cap of the globe they touch.
     */

    // Internal
    private TextureManager textureManager;
    private BiomeManager biomeManager;
    private MapGenerationBranch mapGenerationBranch;
    private MapChunkBranch mapChunkBranch;
    private MapOverlayBranch mapOverlayBranch;
    private MapMarkerBranch mapMarkerBranch;

    // World
    private WorldHandle worldHandle;
    private long worldWidthBlocks;
    private long worldHeightBlocks;
    private int maxLevel;

    // Globe — the block face's side the tiles divide on a globe, else the world's own extent
    private boolean globe;
    private long spanWidthBlocks;
    private long spanHeightBlocks;
    private double radiusBlocks;

    // Palette
    private Long2ObjectOpenHashMap<MapTileInstance> key2MapTile;
    private ObjectArrayList<MapTileInstance> tilePool;
    private ObjectArrayList<MapTileInstance> retiredTiles;
    private ObjectArrayList<MapTileInstance> shownFinestTiles;
    private long frame;

    // Live Edits
    private int shownBiomeRevision;

    // Preview — read by tile workers, so a new one is published whole
    private volatile WorldHandle previewWorldHandle;
    private volatile LayoutPlanStruct previewLayout;

    // Scratch
    private ByteBuffer uploadBuffer;

    // Drawable — the texture and its part a tile draws with, found by resolveDrawable
    private int drawableTexture;
    private float drawableU0;
    private float drawableV0;
    private float drawableU1;
    private float drawableV1;

    // Globe Scratch
    private final Vector3Double globeDirection = new Vector3Double();
    private final GlobeCapStruct editCap = new GlobeCapStruct();

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.mapGenerationBranch = create(MapGenerationBranch.class);
        this.mapChunkBranch = create(MapChunkBranch.class);
        this.mapOverlayBranch = create(MapOverlayBranch.class);
        this.mapMarkerBranch = create(MapMarkerBranch.class);

        // Palette
        this.key2MapTile = new Long2ObjectOpenHashMap<>();
        this.tilePool = new ObjectArrayList<>();
        this.retiredTiles = new ObjectArrayList<>();
        this.shownFinestTiles = new ObjectArrayList<>();

        // Scratch
        this.uploadBuffer = BufferUtility.newByteBuffer(
                EngineSetting.MAP_TILE_TEXELS * EngineSetting.MAP_TILE_TEXELS * EngineSetting.COLOR_CHANNEL_COUNT);
    }

    @Override
    protected void get() {
        this.textureManager = get(TextureManager.class);
        this.biomeManager = get(BiomeManager.class);
    }

    @Override
    protected void dispose() {

        retireAllTiles();

        for (int i = 0; i < tilePool.size(); i++)
            textureManager.deleteTexture2D(tilePool.get(i).getTexture());

        for (int i = 0; i < retiredTiles.size(); i++)
            textureManager.deleteTexture2D(retiredTiles.get(i).getTexture());

        tilePool.clear();
        retiredTiles.clear();
    }

    // Update \\

    @Override
    protected void update() {

        frame++;
        recycleRetiredTiles();
        syncBiomeRevision();

        if (worldHandle == null)
            return;

        uploadGeneratedTiles();
        refreshShownChunks();
        evictStaleTiles();
        mapOverlayBranch.update(worldHandle, frame);
    }

    // World \\

    private void resolveWorld(WorldHandle nextWorldHandle) {

        if (nextWorldHandle == worldHandle)
            return;

        retireAllTiles();

        this.worldHandle = nextWorldHandle;
        this.worldWidthBlocks = nextWorldHandle.getWorldScale().x;
        this.worldHeightBlocks = nextWorldHandle.getWorldScale().y;
        this.globe = nextWorldHandle.isGlobe();
        this.spanWidthBlocks = globe ? GlobeUtility.getFaceBlocks(nextWorldHandle) : worldWidthBlocks;
        this.spanHeightBlocks = globe ? GlobeUtility.getFaceBlocks(nextWorldHandle) : worldHeightBlocks;
        this.radiusBlocks = globe ? GlobeUtility.getRadiusBlocks(nextWorldHandle) : 0.0;

        long finestTileBlocks = EngineSetting.MACRO_TILE_SIZE_BLOCKS;

        if (spanWidthBlocks % finestTileBlocks != 0 || spanHeightBlocks % finestTileBlocks != 0)
            throwException("World \"" + nextWorldHandle.getWorldName() + "\" spans " + spanWidthBlocks + "x"
                    + spanHeightBlocks + " blocks " + (globe ? "a face" : "") + ", which the map's finest tile ("
                    + finestTileBlocks + " blocks) does not divide.");

        // Every level's tiles must divide the world, or a globe's face, on both axes, so level zero is as coarse as
        // that allows
        int levels = 0;

        while (levels + 1 < EngineSetting.MAP_MAX_LEVELS
                && spanWidthBlocks % (finestTileBlocks << (levels + 1)) == 0
                && spanHeightBlocks % (finestTileBlocks << (levels + 1)) == 0)
            levels++;

        this.maxLevel = levels;
    }

    private long getTileBlocks(int level) {
        return (long) EngineSetting.MACRO_TILE_SIZE_BLOCKS << (maxLevel - level);
    }

    // Tiles across the world, or across one face of a globe
    private int getTilesAcrossX(int level) {
        return (int) (spanWidthBlocks / getTileBlocks(level));
    }

    private int getTilesAcrossZ(int level) {
        return (int) (spanHeightBlocks / getTileBlocks(level));
    }

    // The first tile of a face along x, a globe's faces packed into x; a torus has one span from zero
    private int getFaceOriginTile(int face, int level) {
        return globe ? (int) (GlobeUtility.faceOriginX(face, (int) spanWidthBlocks) / getTileBlocks(level)) : 0;
    }

    private int getSpanCount() {
        return globe ? EngineSetting.GLOBE_FACE_COUNT : 1;
    }

    // The coarsest level whose texels are no wider than a pixel, so a tile is never stretched past its detail
    private int selectLevel(double blocksPerPixel) {

        for (int level = 0; level < maxLevel; level++)
            if (getTileBlocks(level) / (double) EngineSetting.MAP_TILE_TEXELS <= blocksPerPixel)
                return level;

        return maxLevel;
    }

    private long toKey(int level, int tileX, int tileZ) {
        return ((long) level << EngineSetting.MAP_TILE_KEY_LEVEL_SHIFT)
                | (((long) tileX & EngineSetting.MAP_TILE_KEY_AXIS_MASK) << EngineSetting.MAP_TILE_KEY_X_SHIFT)
                | ((long) tileZ & EngineSetting.MAP_TILE_KEY_AXIS_MASK);
    }

    // View \\

    public void resolveView(MapViewStruct view) {

        resolveWorld(view.getWorldHandle());

        if (globe) {
            resolveGlobeView(view);
            return;
        }

        int level = selectLevel(view.getBlocksPerPixel());
        long tileBlocks = getTileBlocks(level);

        view.beginDraws(level);
        requestBaseTiles();
        requestOverlays(view);

        long firstX = (long) Math.floor(view.screenToWorldX(0f) / tileBlocks);
        long lastX = (long) Math.floor(view.screenToWorldX(view.getWidth()) / tileBlocks);
        long firstZ = (long) Math.floor(view.screenToWorldZ(view.getHeight()) / tileBlocks);
        long lastZ = (long) Math.floor(view.screenToWorldZ(0f) / tileBlocks);

        for (long tileZ = firstZ; tileZ <= lastZ; tileZ++)
            for (long tileX = firstX; tileX <= lastX; tileX++)
                resolveViewTile(view, level, tileX, tileZ, tileBlocks);
    }

    // tileX and tileZ are unwrapped, so a tile repeated across the world's wrap lands where the view expects it
    private void resolveViewTile(MapViewStruct view, int level, long tileX, long tileZ, long tileBlocks) {

        int wrappedX = (int) Math.floorMod(tileX, (long) getTilesAcrossX(level));
        int wrappedZ = (int) Math.floorMod(tileZ, (long) getTilesAcrossZ(level));

        if (!resolveDrawable(level, wrappedX, wrappedZ))
            return;

        MapDrawStruct draw = view.nextDraw();

        draw.set(
                drawableTexture,
                view.worldToScreenX((double) tileX * tileBlocks),
                view.worldToScreenY((double) (tileZ + 1) * tileBlocks),
                view.worldToScreenX((double) (tileX + 1) * tileBlocks),
                view.worldToScreenY((double) tileZ * tileBlocks));
        draw.setRegion(drawableU0, drawableV0, drawableU1, drawableV1);
        setWorldRegion(draw, tileX, tileZ, tileBlocks);
    }

    // The texture a tile draws with: its own once uploaded, else the matching part of its nearest uploaded ancestor;
    // false while neither is ready
    private boolean resolveDrawable(int level, int tileX, int tileZ) {

        MapTileInstance tile = acquireTile(level, tileX, tileZ);

        if (tile.isUploaded()) {

            setDrawable(tile.getTexture(), 0f, 0f, 1f, 1f);

            if (level == maxLevel && tile.markShownFinest(frame))
                shownFinestTiles.add(tile);

            return true;
        }

        for (int ancestorLevel = level - 1; ancestorLevel >= 0; ancestorLevel--) {

            int depth = level - ancestorLevel;
            MapTileInstance ancestor = key2MapTile.get(toKey(ancestorLevel, tileX >> depth, tileZ >> depth));

            if (ancestor == null || !ancestor.isUploaded())
                continue;

            ancestor.touch(frame);

            float span = 1 << depth;
            float u0 = (tileX - ((tileX >> depth) << depth)) / span;
            float v0 = (tileZ - ((tileZ >> depth) << depth)) / span;

            setDrawable(ancestor.getTexture(), u0, v0, u0 + 1f / span, v0 + 1f / span);

            return true;
        }

        return false;
    }

    private void setDrawable(int texture, float u0, float v0, float u1, float v1) {
        this.drawableTexture = texture;
        this.drawableU0 = u0;
        this.drawableV0 = v0;
        this.drawableU1 = u1;
        this.drawableV1 = v1;
    }

    // Globe View \\

    // Every face's tiles refined from level zero, only those the camera sees drawn
    private void resolveGlobeView(MapViewStruct view) {

        view.beginDraws(0);
        requestBaseTiles();

        for (int face = 0; face < EngineSetting.GLOBE_FACE_COUNT; face++)
            for (int tileZ = 0; tileZ < getTilesAcrossZ(0); tileZ++)
                for (int tileX = 0; tileX < getTilesAcrossX(0); tileX++)
                    refineGlobeTile(view, face, 0, getFaceOriginTile(face, 0) + tileX, tileZ);
    }

    // A tile beyond the horizon or off the window is skipped, one showing a texel wider than a pixel splits into its
    // four children, and any other is drawn
    private void refineGlobeTile(MapViewStruct view, int face, int level, int tileX, int tileZ) {

        long tileBlocks = getTileBlocks(level);
        GlobeViewStruct globeView = view.getGlobeView();
        Vector3 camera = globeView.getPosition();
        double cameraDistance = globeView.getCameraDistance();

        GlobeUtility.resolveDirection(worldHandle, (tileX + 0.5) * tileBlocks, (tileZ + 0.5) * tileBlocks,
                globeDirection);

        double reach = tileBlocks * EngineSetting.MAP_GLOBE_TILE_REACH / radiusBlocks;
        double towardCamera = (globeDirection.x * camera.x + globeDirection.y * camera.y
                + globeDirection.z * camera.z) / cameraDistance;
        double horizon = Math.acos(Math.min(1.0, 1.0 / cameraDistance));

        if (Math.acos(Math.max(-1.0, Math.min(1.0, towardCamera))) - reach > horizon)
            return;

        double distance = globeView.distanceTo(globeDirection.x, globeDirection.y, globeDirection.z);
        double nearest = Math.max(distance - reach, EngineSetting.DIVISION_EPSILON);
        double shownPixels = tileBlocks / radiusBlocks / globeView.resolveSpanPerPixel(nearest, view.getHeight());

        if (isOffWindow(globeView, distance, reach))
            return;

        if (level < maxLevel && shownPixels > EngineSetting.MAP_TILE_TEXELS) {

            for (int childZ = 0; childZ < 2; childZ++)
                for (int childX = 0; childX < 2; childX++)
                    refineGlobeTile(view, face, level + 1, tileX * 2 + childX, tileZ * 2 + childZ);

            return;
        }

        drawGlobeTile(view, face, level, tileX, tileZ, tileBlocks);
    }

    // True for a tile, its middle the direction last resolved, lying wholly outside the camera's view: the angle
    // from the view's axis to its middle, less the angle the tile spans from the camera, beyond the view's widest
    // reach, a corner of the window; never for a tile the camera stands within reach of
    private boolean isOffWindow(GlobeViewStruct globeView, double distance, double reach) {

        if (distance <= reach)
            return false;

        Vector3 camera = globeView.getPosition();
        Vector3 forward = globeView.getForward();
        Vector3 right = globeView.getRight();
        Vector3 up = globeView.getUp();
        double along = ((globeDirection.x - camera.x) * forward.x + (globeDirection.y - camera.y) * forward.y
                + (globeDirection.z - camera.z) * forward.z) / distance;
        double fromAxis = Math.acos(Math.max(-1.0, Math.min(1.0, along)));
        double spanned = Math.asin(reach / distance);
        double viewReach = Math.atan(Math.sqrt(right.x * right.x + right.y * right.y + right.z * right.z
                + up.x * up.x + up.y * up.y + up.z * up.z));

        return fromAxis - spanned > viewReach;
    }

    // The window rectangle a tile shows in, from where its corners, edges and middle fall, widened for the bow of its
    // edges between them; the whole window when any of them lies behind the camera
    private void drawGlobeTile(MapViewStruct view, int face, int level, int tileX, int tileZ, long tileBlocks) {

        if (!resolveDrawable(level, tileX, tileZ))
            return;

        GlobeViewStruct globeView = view.getGlobeView();
        float left = Float.MAX_VALUE;
        float bottom = Float.MAX_VALUE;
        float right = -Float.MAX_VALUE;
        float top = -Float.MAX_VALUE;
        boolean behind = false;
        int steps = EngineSetting.MAP_GLOBE_TILE_BOUND_STEPS;

        for (int stepZ = 0; stepZ <= steps && !behind; stepZ++) {
            for (int stepX = 0; stepX <= steps && !behind; stepX++) {

                GlobeUtility.resolveDirection(
                        worldHandle,
                        (tileX + stepX / (double) steps) * tileBlocks,
                        (tileZ + stepZ / (double) steps) * tileBlocks,
                        globeDirection);

                behind = !globeView.project(
                        globeDirection.x, globeDirection.y, globeDirection.z, view.getWidth(), view.getHeight());
                left = Math.min(left, globeView.getProjectedX());
                bottom = Math.min(bottom, globeView.getProjectedY());
                right = Math.max(right, globeView.getProjectedX());
                top = Math.max(top, globeView.getProjectedY());
            }
        }

        float bow = Math.max(right - left, top - bottom) * EngineSetting.MAP_GLOBE_TILE_BOW_SHARE
                + EngineSetting.MAP_GLOBE_TILE_BOW_PIXELS;

        left = behind ? 0f : Math.max(0f, left - bow);
        bottom = behind ? 0f : Math.max(0f, bottom - bow);
        right = behind ? view.getWidth() : Math.min(view.getWidth(), right + bow);
        top = behind ? view.getHeight() : Math.min(view.getHeight(), top + bow);

        if (right <= left || top <= bottom)
            return;

        long faceOrigin = GlobeUtility.faceOriginX(face, (int) spanWidthBlocks);
        MapDrawStruct draw = view.nextDraw();

        draw.set(drawableTexture, left, bottom, right, top);
        draw.setRegion(drawableU0, drawableV0, drawableU1, drawableV1);
        draw.setFace(face);
        draw.setWorldRegion(
                (float) ((tileX * tileBlocks - faceOrigin) / (double) spanWidthBlocks),
                (float) (tileZ * tileBlocks / (double) spanHeightBlocks),
                (float) (((tileX + 1) * tileBlocks - faceOrigin) / (double) spanWidthBlocks),
                (float) ((tileZ + 1) * tileBlocks / (double) spanHeightBlocks));
    }

    private void setWorldRegion(MapDrawStruct draw, long tileX, long tileZ, long tileBlocks) {
        draw.setWorldRegion(
                (float) ((double) tileX * tileBlocks / worldWidthBlocks),
                (float) ((double) tileZ * tileBlocks / worldHeightBlocks),
                (float) ((double) (tileX + 1) * tileBlocks / worldWidthBlocks),
                (float) ((double) (tileZ + 1) * tileBlocks / worldHeightBlocks));
    }

    // Overlays \\

    private void requestOverlays(MapViewStruct view) {

        if (view.isShowingDayNight())
            mapOverlayBranch.requestDayNight(frame);

        if (view.isShowingWeather())
            mapOverlayBranch.requestWeather(frame);
    }

    // Markers \\

    public int resolveMarkers(WorldHandle markerWorldHandle, ObjectArrayList<MapMarkerStruct> markers) {
        return mapMarkerBranch.resolveMarkers(markerWorldHandle, markers);
    }

    public void resolveMarker(GridInstance grid, MapMarkerStruct marker) {
        mapMarkerBranch.resolveMarker(grid, marker);
    }

    // Live Edits \\

    // Pixels are inclusive world image pixels; tiles they reach through the biome field's blend regenerate
    public void invalidateWorldPixels(
            WorldHandle editedWorldHandle,
            int minPixelX,
            int minPixelZ,
            int maxPixelX,
            int maxPixelZ) {

        if (editedWorldHandle != worldHandle)
            return;

        double pixelBlocks = EngineSetting.CHUNKS_PER_PIXEL * (double) EngineSetting.CHUNK_SIZE;
        int margin = EngineSetting.MAP_EDIT_MARGIN_PIXELS;

        if (globe) {
            invalidateGlobePixels(minPixelX, minPixelZ, maxPixelX, maxPixelZ, margin * pixelBlocks / radiusBlocks);
            return;
        }

        invalidateWorldBlocks(
                (minPixelX - margin) * pixelBlocks,
                (minPixelZ - margin) * pixelBlocks,
                (maxPixelX + 1 + margin) * pixelBlocks,
                (maxPixelZ + 1 + margin) * pixelBlocks);
    }

    // Every tile a block region reaches, measured with the lattice margin its generation samples beyond it
    private void invalidateWorldBlocks(double minBlockX, double minBlockZ, double maxBlockX, double maxBlockZ) {

        for (MapTileInstance tile : key2MapTile.values()) {

            long tileBlocks = getTileBlocks(tile.getLevel());
            double reach = mapGenerationBranch.resolveSpacing(tileBlocks);
            double tileMinX = (double) tile.getTileX() * tileBlocks - reach;
            double tileMinZ = (double) tile.getTileZ() * tileBlocks - reach;

            if (overlapsWrapped(tileMinX, tileMinX + tileBlocks + reach * 2, minBlockX, maxBlockX, worldWidthBlocks)
                    && overlapsWrapped(
                            tileMinZ, tileMinZ + tileBlocks + reach * 2, minBlockZ, maxBlockZ, worldHeightBlocks))
                tile.markStale();
        }
    }

    // Every tile whose square of its face meets the cap of the globe the pixels touch
    private void invalidateGlobePixels(int minPixelX, int minPixelZ, int maxPixelX, int maxPixelZ, double margin) {

        editCap.clear();
        editCap.includeImagePixels(worldHandle, minPixelX, minPixelZ, maxPixelX, maxPixelZ, margin);

        for (MapTileInstance tile : key2MapTile.values()) {

            long tileBlocks = getTileBlocks(tile.getLevel());

            GlobeUtility.resolveDirection(
                    worldHandle, (tile.getTileX() + 0.5) * tileBlocks, (tile.getTileZ() + 0.5) * tileBlocks,
                    globeDirection);

            if (editCap.reaches(globeDirection.x, globeDirection.y, globeDirection.z,
                    tileBlocks * EngineSetting.MAP_GLOBE_TILE_REACH / radiusBlocks))
                tile.markStale();
        }
    }

    // Preview \\

    // The one layout the editor previews, null for none, the tiles under the old and the new one regenerated
    public void setPreviewLayout(WorldHandle previewWorld, LayoutPlanStruct layout) {

        invalidatePreview();

        this.previewLayout = null;
        this.previewWorldHandle = previewWorld;
        this.previewLayout = layout;

        invalidatePreview();
    }

    private void invalidatePreview() {

        LayoutPlanStruct layout = previewLayout;

        if (layout == null || previewWorldHandle != worldHandle)
            return;

        invalidateWorldBlocks(layout.getMinX(), layout.getMinZ(), layout.getMaxX(), layout.getMaxZ());
    }

    // Worker — the previewed layout when it stands in this world, null otherwise
    LayoutPlanStruct getPreviewLayout(WorldHandle tileWorldHandle) {

        LayoutPlanStruct layout = previewLayout;

        return layout != null && previewWorldHandle == tileWorldHandle ? layout : null;
    }

    // A rebuilt biome can change any part of the field, so every tile regenerates
    private void syncBiomeRevision() {

        int biomeRevision = biomeManager.getRevision();

        if (biomeRevision == shownBiomeRevision)
            return;

        this.shownBiomeRevision = biomeRevision;

        for (MapTileInstance tile : key2MapTile.values())
            tile.markStale();
    }

    // Tests the interval against the region and its copies one world over on either side
    // A globe's faces lie apart along x and never wrap, so a span there overlaps only where it stands
    private boolean overlapsWrapped(double min, double max, double regionMin, double regionMax, double period) {

        if (globe)
            return min < regionMax && regionMin < max;

        if (regionMax - regionMin >= period)
            return true;

        double shift = Math.floor(regionMin / period) * period;
        double wrappedMin = regionMin - shift;
        double wrappedMax = regionMax - shift;

        for (int copy = -1; copy <= 1; copy++)
            if (min < wrappedMax + copy * period && wrappedMin + copy * period < max)
                return true;

        return false;
    }

    // Tiles \\

    // Level zero across the world, or across every face of a globe
    private void requestBaseTiles() {

        for (int span = 0; span < getSpanCount(); span++)
            for (int tileZ = 0; tileZ < getTilesAcrossZ(0); tileZ++)
                for (int tileX = 0; tileX < getTilesAcrossX(0); tileX++)
                    acquireTile(0, getFaceOriginTile(span, 0) + tileX, tileZ);
    }

    private MapTileInstance acquireTile(int level, int tileX, int tileZ) {

        long key = toKey(level, tileX, tileZ);
        MapTileInstance tile = key2MapTile.get(key);

        if (tile == null) {
            tile = obtainPooledTile();
            tile.constructor(level, tileX, tileZ, key, tile.getTexture());
            key2MapTile.put(key, tile);
        }

        tile.touch(frame);

        if (!tile.isRequested() && mapGenerationBranch.hasCapacity())
            mapGenerationBranch.generateTile(tile, worldHandle, getTileBlocks(level));

        return tile;
    }

    private MapTileInstance obtainPooledTile() {

        if (!tilePool.isEmpty())
            return tilePool.pop();

        MapTileInstance tile = create(MapTileInstance.class);
        int texture = textureManager.createTexture2D(
                EngineSetting.MAP_TILE_TEXELS,
                EngineSetting.MAP_TILE_TEXELS,
                EngineSetting.GL_CLAMP_TO_EDGE,
                EngineSetting.GL_NEAREST);

        tile.constructor(0, 0, 0, 0L, texture);

        return tile;
    }

    // Upload \\

    private void uploadGeneratedTiles() {

        int uploads = 0;
        ObjectIterator<Long2ObjectMap.Entry<MapTileInstance>> iterator = key2MapTile.long2ObjectEntrySet()
                .fastIterator();

        while (iterator.hasNext() && uploads < EngineSetting.MAP_TILE_UPLOADS_PER_FRAME) {

            MapTileInstance tile = iterator.next().getValue();

            if (!tile.isAwaitingUpload())
                continue;

            uploadBuffer.clear();
            uploadBuffer.put(tile.getPixels());
            textureManager.updateTexture2D(
                    tile.getTexture(), 0, 0, EngineSetting.MAP_TILE_TEXELS, EngineSetting.MAP_TILE_TEXELS,
                    uploadBuffer);

            tile.markUploaded();
            uploads++;
        }
    }

    // Chunks \\

    private void refreshShownChunks() {

        int budget = EngineSetting.MAP_CHUNKS_PER_FRAME;

        for (int i = 0; i < shownFinestTiles.size() && budget > 0; i++)
            budget = mapChunkBranch.refreshTile(shownFinestTiles.get(i), worldHandle, budget);

        shownFinestTiles.clear();
    }

    // Eviction \\

    // Least recently shown first; level zero, tiles a view showed this frame or last, and tiles generating are kept
    private void evictStaleTiles() {

        while (key2MapTile.size() > EngineSetting.MAP_TILE_CACHE_MAX) {

            MapTileInstance stalest = null;

            for (MapTileInstance tile : key2MapTile.values()) {

                if (tile.getLevel() == 0 || tile.isGenerating() || tile.getLastUsedFrame() >= frame - 1)
                    continue;

                if (stalest == null || tile.getLastUsedFrame() < stalest.getLastUsedFrame())
                    stalest = tile;
            }

            if (stalest == null)
                return;

            key2MapTile.remove(stalest.getKey());
            tilePool.push(stalest);
        }
    }

    // A tile still generating keeps its worker's writes to itself until it finishes, then joins the pool
    private void retireAllTiles() {

        for (MapTileInstance tile : key2MapTile.values()) {

            if (tile.isGenerating())
                retiredTiles.add(tile);
            else
                tilePool.push(tile);
        }

        key2MapTile.clear();
        shownFinestTiles.clear();
    }

    private void recycleRetiredTiles() {

        for (int i = retiredTiles.size() - 1; i >= 0; i--) {

            MapTileInstance tile = retiredTiles.get(i);

            if (!tile.isAwaitingUpload())
                continue;

            tile.markUploaded();
            tilePool.push(tile);
            retiredTiles.remove(i);
        }
    }

    // Accessible \\

    public int getMaxLevel() {
        return maxLevel;
    }

    public boolean hasDayNightOverlay() {
        return mapOverlayBranch.isDaylightReady();
    }

    public boolean hasWeatherOverlay() {
        return mapOverlayBranch.isWeatherReady();
    }

    public int getDaylightTexture() {
        return mapOverlayBranch.getDaylightTexture();
    }

    public int getWeatherTexture() {
        return mapOverlayBranch.getWeatherTexture();
    }

    public float getWeatherOffsetU() {
        return mapOverlayBranch.getWeatherOffsetU();
    }

    public float getWeatherOffsetV() {
        return mapOverlayBranch.getWeatherOffsetV();
    }
}
