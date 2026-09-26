package application.bootstrap.worldpipeline.worlditemplacementsystem;

import application.bootstrap.itempipeline.item.ItemInstance;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.itempipeline.itemdefinitionmanager.ItemDefinitionManager;
import application.bootstrap.itempipeline.itemmanager.ItemManager;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worlditem.WorldItemInstance;
import application.bootstrap.worldpipeline.worlditem.WorldItemInstancePaletteHandle;
import application.bootstrap.worldpipeline.worlditem.WorldItemPaletteHandle;
import application.bootstrap.worldpipeline.worlditem.WorldItemStruct;
import application.bootstrap.worldpipeline.worlditemrendersystem.WorldItemRenderSystem;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.extras.Coordinate3Int;
import engine.util.mathematics.extras.Coordinate4Long;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/*
 * Central entry point for all world item placement and removal.
 *
 * Chunk load flow (branch-driven):
 *   buildChunkInstances  — async safe, CPU only, builds palette from subchunk structs
 *   pushChunkToRenderer  — main thread, pushes built palette to renderer
 *   pullChunkFromRenderer — main thread, removes chunk from renderer
 *
 * Runtime flow (player/entity):
 *   placeItem  — all three layers: SubChunk → Chunk palette → Renderer
 *   removeItem — all three layers in reverse
 *   raycastItem — the nearest item a ray meets across the chunk it starts in
 *                 and that chunk's neighbours, each item filling its block cube
 *   resolveItemInstance — the real item standing at a world item, made on
 *                 first need and kept on its subchunk struct so a chest's
 *                 contents outlive any rebuild of the chunk palette
 */
public class WorldItemPlacementSystem extends ManagerPackage {

    // Internal
    private ItemDefinitionManager itemDefinitionManager;
    private ItemManager itemManager;
    private WorldItemRenderSystem worldItemRenderSystem;
    private WorldStreamManager worldStreamManager;

    // Internal \\

    @Override
    protected void get() {
        this.itemDefinitionManager = get(ItemDefinitionManager.class);
        this.itemManager = get(ItemManager.class);
        this.worldItemRenderSystem = get(WorldItemRenderSystem.class);
        this.worldStreamManager = get(WorldStreamManager.class);
    }

    // Chunk Load Flow \\

    /*
     * CPU only — safe to call under async lock.
     * Builds WorldItemInstances from subchunk structs into the chunk palette.
     * Does not touch the renderer.
     */
    public void buildChunkInstances(ChunkInstance chunk, long chunkCoordinate) {
        WorldItemInstancePaletteHandle palette = chunk.getWorldItemInstancePaletteHandle();
        palette.clear();
        SubChunkInstance[] subChunks = chunk.getSubChunks();
        for (int i = 0; i < EngineSetting.WORLD_HEIGHT; i++) {
            WorldItemPaletteHandle structPalette = subChunks[i].getWorldItemPaletteHandle();
            if (structPalette.isEmpty())
                continue;
            ObjectArrayList<WorldItemStruct> structs = structPalette.getItems();
            for (int j = 0; j < structs.size(); j++) {
                WorldItemInstance instance = buildInstance(structs.get(j), chunkCoordinate);
                if (instance != null)
                    palette.addItem(instance);
            }
        }
    }

    /*
     * Main thread only — pushes the chunk's built palette to the renderer.
     * Always called after buildChunkInstances has completed.
     */
    public void pushChunkToRenderer(ChunkInstance chunk, long chunkCoordinate) {
        worldItemRenderSystem.push(chunkCoordinate,
                chunk.getWorldItemInstancePaletteHandle().getItems());
    }

    /*
     * Main thread only — removes all items for this chunk from the renderer.
     * Chunk palette is cleared separately by DumpBranch.dumpItemData.
     */
    public void pullChunkFromRenderer(long chunkCoordinate) {
        worldItemRenderSystem.pull(chunkCoordinate);
    }

    // Runtime Placement \\

    public WorldItemInstance placeItem(
            ChunkInstance chunk,
            int subChunkCoordinate,
            long packedPosition,
            ItemInstance itemInstance) {

        ItemDefinitionHandle def = itemInstance.getItemDefinitionHandle();

        // 1. SubChunk
        WorldItemStruct struct = new WorldItemStruct(packedPosition, def.getItemID(), itemInstance);
        SubChunkInstance subChunk = chunk.getSubChunk(subChunkCoordinate);
        subChunk.getWorldItemPaletteHandle().addItem(struct);

        // 2. Chunk
        long chunkCoordinate = chunk.getCoordinate();
        WorldItemInstance instance = buildInstance(struct, chunkCoordinate, def);
        chunk.getWorldItemInstancePaletteHandle().addItem(instance);

        // 3. Renderer
        worldItemRenderSystem.addItem(instance, chunkCoordinate);

        return instance;
    }

    // Runtime Removal \\

    public void removeItem(ChunkInstance chunk, WorldItemInstance instance) {

        // 1. Renderer
        worldItemRenderSystem.removeItem(instance);

        // 2. Chunk
        chunk.getWorldItemInstancePaletteHandle().removeItem(instance);

        // 3. SubChunk
        int subY = Coordinate4Long.unpackY(instance.getPackedPosition());
        int subChunkCoordinate = (subY / EngineSetting.SUB_VOXEL_RESOLUTION) / EngineSetting.CHUNK_SIZE;
        SubChunkInstance subChunk = chunk.getSubChunk(subChunkCoordinate);
        removeMatchingStruct(subChunk, instance.getPackedPosition(), instance.getPackedItem());
    }

    // Runtime Pick \\

    public WorldItemInstance raycastItem(
            WorldHandle worldHandle,
            long chunkCoordinate,
            Vector3 origin,
            Vector3 direction,
            float maxDistance) {

        WorldItemInstance nearest = null;
        float nearestDistance = maxDistance;

        for (int offsetZ = -1; offsetZ <= 1; offsetZ++)
            for (int offsetX = -1; offsetX <= 1; offsetX++) {

                long neighbourCoordinate = WorldWrapUtility.wrapAroundWorld(
                        worldHandle, Coordinate2Long.add(chunkCoordinate, offsetX, offsetZ));
                ChunkInstance chunk = worldStreamManager.getChunkInstance(neighbourCoordinate);

                // A palette being rebuilt on the streaming thread is skipped for this frame
                if (chunk == null || !chunk.getChunkDataSyncContainer().tryAcquire())
                    continue;

                try {

                    ObjectArrayList<WorldItemInstance> items = chunk.getWorldItemInstancePaletteHandle().getItems();

                    for (int i = 0; i < items.size(); i++) {

                        float distance = intersectItemCube(
                                items.get(i),
                                offsetX * EngineSetting.CHUNK_SIZE,
                                offsetZ * EngineSetting.CHUNK_SIZE,
                                origin,
                                direction);

                        if (distance >= nearestDistance)
                            continue;

                        nearest = items.get(i);
                        nearestDistance = distance;
                    }
                } finally {
                    chunk.getChunkDataSyncContainer().release();
                }
            }

        return nearest;
    }

    // Distance along the ray to the item's block cube, Float.MAX_VALUE on a miss
    private float intersectItemCube(
            WorldItemInstance instance,
            float chunkOffsetX,
            float chunkOffsetZ,
            Vector3 origin,
            Vector3 direction) {

        float svr = EngineSetting.SUB_VOXEL_RESOLUTION;
        long packed = instance.getPackedPosition();
        float[] mins = {
                chunkOffsetX + Coordinate4Long.unpackX(packed) / svr,
                Coordinate4Long.unpackY(packed) / svr,
                chunkOffsetZ + Coordinate4Long.unpackZ(packed) / svr };
        float[] origins = { origin.x, origin.y, origin.z };
        float[] directions = { direction.x, direction.y, direction.z };

        float near = 0f;
        float far = Float.MAX_VALUE;

        for (int axis = 0; axis < 3; axis++) {

            if (directions[axis] == 0f) {

                if (origins[axis] < mins[axis] || origins[axis] > mins[axis] + 1f)
                    return Float.MAX_VALUE;

                continue;
            }

            float first = (mins[axis] - origins[axis]) / directions[axis];
            float second = (mins[axis] + 1f - origins[axis]) / directions[axis];

            near = Math.max(near, Math.min(first, second));
            far = Math.min(far, Math.max(first, second));
        }

        return near <= far ? near : Float.MAX_VALUE;
    }

    public ItemInstance resolveItemInstance(WorldItemInstance instance) {

        if (!instance.hasItemInstance())
            instance.setItemInstance(itemManager.createItem(instance.getItemDefinitionHandle()));

        return instance.getItemInstance();
    }

    // Build \\

    private WorldItemInstance buildInstance(WorldItemStruct struct, long chunkCoordinate) {
        int itemID = struct.packedItem & 0xFFFF0000;
        if (itemID == EngineSetting.REGISTRY_RESERVED_ID)
            return null;
        ItemDefinitionHandle def = itemDefinitionManager.getItemHandleFromItemID(itemID);
        if (def == null)
            return null;
        return buildInstance(struct, chunkCoordinate, def);
    }

    private WorldItemInstance buildInstance(
            WorldItemStruct struct,
            long chunkCoordinate,
            ItemDefinitionHandle def) {
        int SVR = EngineSetting.SUB_VOXEL_RESOLUTION;
        int subX = Coordinate4Long.unpackX(struct.packedPosition);
        int subY = Coordinate4Long.unpackY(struct.packedPosition);
        int subZ = Coordinate4Long.unpackZ(struct.packedPosition);
        int packedBlockCoordinate = Coordinate3Int.pack(subX / SVR, subY / SVR, subZ / SVR);
        WorldItemInstance instance = create(WorldItemInstance.class);
        instance.constructor(struct, def, chunkCoordinate, packedBlockCoordinate,
                struct.packedPosition, struct.packedItem);
        return instance;
    }

    // Helpers \\

    private void removeMatchingStruct(
            SubChunkInstance subChunk,
            long packedPosition,
            int packedItem) {
        WorldItemPaletteHandle palette = subChunk.getWorldItemPaletteHandle();
        ObjectArrayList<WorldItemStruct> structs = palette.getItems();
        for (int i = 0; i < structs.size(); i++) {
            WorldItemStruct s = structs.get(i);
            if (s.packedPosition == packedPosition && s.packedItem == packedItem) {
                palette.removeItem(s);
                return;
            }
        }
    }
}