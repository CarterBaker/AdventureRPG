package application.bootstrap.mappipeline.mapmanager;

import java.nio.ByteBuffer;

import application.bootstrap.mappipeline.map.MapDrawStruct;
import application.bootstrap.mappipeline.map.MapTileInstance;
import application.bootstrap.mappipeline.map.MapViewStruct;
import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
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
     * whole world shows at once. Tile indices wrap with the world.
     */

    // Internal
    private TextureManager textureManager;
    private MapGenerationBranch mapGenerationBranch;
    private MapChunkBranch mapChunkBranch;

    // World
    private WorldHandle worldHandle;
    private long worldWidthBlocks;
    private long worldHeightBlocks;
    private int maxLevel;

    // Palette
    private Long2ObjectOpenHashMap<MapTileInstance> key2MapTile;
    private ObjectArrayList<MapTileInstance> tilePool;
    private ObjectArrayList<MapTileInstance> retiredTiles;
    private ObjectArrayList<MapTileInstance> shownFinestTiles;
    private long frame;

    // Scratch
    private ByteBuffer uploadBuffer;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.mapGenerationBranch = create(MapGenerationBranch.class);
        this.mapChunkBranch = create(MapChunkBranch.class);

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

        if (worldHandle == null)
            return;

        uploadGeneratedTiles();
        refreshShownChunks();
        evictStaleTiles();
    }

    // World \\

    private void resolveWorld(WorldHandle nextWorldHandle) {

        if (nextWorldHandle == worldHandle)
            return;

        retireAllTiles();

        this.worldHandle = nextWorldHandle;
        this.worldWidthBlocks = nextWorldHandle.getWorldScale().x;
        this.worldHeightBlocks = nextWorldHandle.getWorldScale().y;

        long finestTileBlocks = EngineSetting.MACRO_TILE_SIZE_BLOCKS;

        if (worldWidthBlocks % finestTileBlocks != 0 || worldHeightBlocks % finestTileBlocks != 0)
            throwException("World \"" + nextWorldHandle.getWorldName() + "\" spans " + worldWidthBlocks + "x"
                    + worldHeightBlocks + " blocks, which the map's finest tile (" + finestTileBlocks
                    + " blocks) does not divide.");

        // Every level's tiles must divide the world on both axes, so level zero is as coarse as that allows
        int levels = 0;

        while (levels + 1 < EngineSetting.MAP_MAX_LEVELS
                && worldWidthBlocks % (finestTileBlocks << (levels + 1)) == 0
                && worldHeightBlocks % (finestTileBlocks << (levels + 1)) == 0)
            levels++;

        this.maxLevel = levels;
    }

    private long getTileBlocks(int level) {
        return (long) EngineSetting.MACRO_TILE_SIZE_BLOCKS << (maxLevel - level);
    }

    private int getTilesAcrossX(int level) {
        return (int) (worldWidthBlocks / getTileBlocks(level));
    }

    private int getTilesAcrossZ(int level) {
        return (int) (worldHeightBlocks / getTileBlocks(level));
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

        int level = selectLevel(view.getBlocksPerPixel());
        long tileBlocks = getTileBlocks(level);

        view.beginDraws(level);
        requestBaseTiles();

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
        MapTileInstance tile = acquireTile(level, wrappedX, wrappedZ);

        float left = view.worldToScreenX((double) tileX * tileBlocks);
        float right = view.worldToScreenX((double) (tileX + 1) * tileBlocks);
        float top = view.worldToScreenY((double) tileZ * tileBlocks);
        float bottom = view.worldToScreenY((double) (tileZ + 1) * tileBlocks);

        if (tile.isUploaded()) {

            view.nextDraw().set(tile.getTexture(), left, bottom, right, top);

            if (level == maxLevel && tile.markShownFinest(frame))
                shownFinestTiles.add(tile);

            return;
        }

        for (int ancestorLevel = level - 1; ancestorLevel >= 0; ancestorLevel--) {

            int depth = level - ancestorLevel;
            MapTileInstance ancestor = key2MapTile.get(toKey(ancestorLevel, wrappedX >> depth, wrappedZ >> depth));

            if (ancestor == null || !ancestor.isUploaded())
                continue;

            ancestor.touch(frame);

            float span = 1 << depth;
            float u0 = (wrappedX - ((wrappedX >> depth) << depth)) / span;
            float v0 = (wrappedZ - ((wrappedZ >> depth) << depth)) / span;
            MapDrawStruct draw = view.nextDraw();

            draw.set(ancestor.getTexture(), left, bottom, right, top);
            draw.setRegion(u0, v0, u0 + 1f / span, v0 + 1f / span);

            return;
        }
    }

    // Tiles \\

    private void requestBaseTiles() {

        for (int tileZ = 0; tileZ < getTilesAcrossZ(0); tileZ++)
            for (int tileX = 0; tileX < getTilesAcrossX(0); tileX++)
                acquireTile(0, tileX, tileZ);
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
            budget = mapChunkBranch.refreshTile(shownFinestTiles.get(i), budget);

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
}
