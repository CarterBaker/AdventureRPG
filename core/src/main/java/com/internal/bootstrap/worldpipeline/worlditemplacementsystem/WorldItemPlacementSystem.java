package com.internal.bootstrap.worldpipeline.worlditemplacementsystem;

import com.internal.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import com.internal.bootstrap.itempipeline.itemdefinitionmanager.ItemDefinitionManager;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import com.internal.bootstrap.worldpipeline.worlditem.WorldItemInstance;
import com.internal.bootstrap.worldpipeline.worlditem.WorldItemInstancePaletteHandle;
import com.internal.bootstrap.worldpipeline.worlditem.WorldItemPaletteHandle;
import com.internal.bootstrap.worldpipeline.worlditem.WorldItemStruct;
import com.internal.bootstrap.worldpipeline.worlditemrendersystem.WorldItemRenderSystem;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.mathematics.Extras.Coordinate3Int;
import com.internal.core.util.mathematics.Extras.Coordinate4Long;
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
 */
public class WorldItemPlacementSystem extends ManagerPackage {

    // Internal
    private ItemDefinitionManager itemDefinitionManager;
    private WorldItemRenderSystem worldItemRenderSystem;

    // Internal \\

    @Override
    protected void get() {
        this.itemDefinitionManager = get(ItemDefinitionManager.class);
        this.worldItemRenderSystem = get(WorldItemRenderSystem.class);
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
            int packedItem,
            ItemDefinitionHandle def) {

        // 1. SubChunk
        WorldItemStruct struct = new WorldItemStruct(packedPosition, packedItem);
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
        int subChunkCoordinate = (subY >> 5) / EngineSetting.CHUNK_SIZE;
        SubChunkInstance subChunk = chunk.getSubChunk(subChunkCoordinate);
        removeMatchingStruct(subChunk, instance.getPackedPosition(), instance.getPackedItem());
    }

    // Build \\

    private WorldItemInstance buildInstance(WorldItemStruct struct, long chunkCoordinate) {
        int itemID = struct.packedItem & 0xFFF;
        ItemDefinitionHandle def = itemDefinitionManager.getItemFromItemID(itemID);
        if (def == null)
            return null;
        return buildInstance(struct, chunkCoordinate, def);
    }

    private WorldItemInstance buildInstance(
            WorldItemStruct struct,
            long chunkCoordinate,
            ItemDefinitionHandle def) {
        int subX = Coordinate4Long.unpackX(struct.packedPosition);
        int subY = Coordinate4Long.unpackY(struct.packedPosition);
        int subZ = Coordinate4Long.unpackZ(struct.packedPosition);
        int packedBlockCoordinate = Coordinate3Int.pack(subX >> 5, subY >> 5, subZ >> 5);
        WorldItemInstance instance = create(WorldItemInstance.class);
        instance.constructor(def, chunkCoordinate, packedBlockCoordinate,
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