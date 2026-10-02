package application.bootstrap.worldpipeline.worlditemmanager;

import application.bootstrap.itempipeline.item.ItemInstance;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.itempipeline.itemdefinitionmanager.ItemDefinitionManager;
import application.bootstrap.itempipeline.itemmanager.ItemManager;
import application.bootstrap.itempipeline.itemrotationmanager.ItemRotationBufferSystem;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worlditem.WorldItemInstance;
import application.bootstrap.worldpipeline.worlditem.WorldItemInstancePaletteHandle;
import application.bootstrap.worldpipeline.worlditem.WorldItemPaletteHandle;
import application.bootstrap.worldpipeline.worlditem.WorldItemPlacementStruct;
import application.bootstrap.worldpipeline.worlditem.WorldItemStruct;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.extras.Coordinate3Int;
import engine.util.mathematics.extras.Coordinate4Long;
import engine.util.mathematics.matrices.Matrix4;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WorldItemPlacementSystem extends SystemPackage {

    /*
     * Single entry point for placing and removing world items, opening them
     * where they stand, and turning one into another item in its place, as a
     * door swings open or a cannon is loaded. Chunk loads stage item palettes
     * off the main thread and commit them as they are pushed to
     * WorldItemRenderSystem on it; runtime placement stands an item on a spot
     * WorldItemSpaceSystem resolved, updating the subchunk, chunk palette and
     * renderer together. resolveItemInstance() keeps an item's real contents
     * on its subchunk struct. composeTransform() is the one CPU-side
     * placement of a world item — its block-space model matrix relative to a
     * chunk, turned exactly as the item shader turns it.
     */

    // Internal
    private ItemDefinitionManager itemDefinitionManager;
    private ItemManager itemManager;
    private ItemRotationBufferSystem itemRotationBufferSystem;
    private WorldItemRenderSystem worldItemRenderSystem;
    private WorldItemSpaceSystem worldItemSpaceSystem;
    private WorldStreamManager worldStreamManager;

    // Scratch
    private WorldItemPlacementStruct placementScratch;

    // Internal \\

    @Override
    protected void create() {

        // Scratch
        this.placementScratch = new WorldItemPlacementStruct();
    }

    @Override
    protected void get() {
        this.itemDefinitionManager = get(ItemDefinitionManager.class);
        this.itemManager = get(ItemManager.class);
        this.itemRotationBufferSystem = get(ItemRotationBufferSystem.class);
        this.worldItemRenderSystem = get(WorldItemRenderSystem.class);
        this.worldItemSpaceSystem = get(WorldItemSpaceSystem.class);
        this.worldStreamManager = get(WorldStreamManager.class);
    }

    // Chunk Load Flow \\

    // Streaming thread, under the chunk's lock — the build is staged, never read until it is committed
    public void buildChunkInstances(ChunkInstance chunk, long chunkCoordinate) {
        WorldItemInstancePaletteHandle palette = chunk.getWorldItemInstancePaletteHandle();
        palette.beginStage();
        SubChunkInstance[] subChunks = chunk.getSubChunks();
        for (int i = 0; i < EngineSetting.WORLD_HEIGHT; i++) {
            WorldItemPaletteHandle structPalette = subChunks[i].getWorldItemPaletteHandle();
            if (structPalette.isEmpty())
                continue;
            ObjectArrayList<WorldItemStruct> structs = structPalette.getItems();
            for (int j = 0; j < structs.size(); j++) {
                WorldItemInstance instance = buildInstance(structs.get(j), chunkCoordinate);
                if (instance != null)
                    palette.stageItem(instance);
            }
        }
    }

    // Main thread, under the chunk's lock — a staged build goes live as it reaches the renderer
    public void pushChunkToRenderer(ChunkInstance chunk, long chunkCoordinate) {
        WorldItemInstancePaletteHandle palette = chunk.getWorldItemInstancePaletteHandle();
        palette.commitStage();
        worldItemRenderSystem.push(chunkCoordinate, palette.getItems());
    }

    public void pullChunkFromRenderer(long chunkCoordinate) {
        worldItemRenderSystem.pull(chunkCoordinate);
    }

    // Runtime Placement \\

    // Stands the item on a spot WorldItemSpaceSystem resolved for its definition — the one way an item enters the world
    public WorldItemInstance placeItem(WorldItemPlacementStruct placementStruct, ItemInstance itemInstance) {

        ItemDefinitionHandle def = itemInstance.getItemDefinitionHandle();
        long chunkCoordinate = placementStruct.getChunkCoordinate();
        long packedPosition = placementStruct.getPackedPosition();
        ChunkInstance chunk = worldStreamManager.getChunkInstance(chunkCoordinate);
        int subChunkCoordinate = (Coordinate4Long.unpackY(packedPosition) / EngineSetting.SUB_VOXEL_RESOLUTION)
                / EngineSetting.CHUNK_SIZE;

        // 1. SubChunk
        WorldItemStruct struct = new WorldItemStruct(packedPosition, def.getItemID(), itemInstance);
        SubChunkInstance subChunk = chunk.getSubChunk(subChunkCoordinate);
        subChunk.getWorldItemPaletteHandle().addItem(struct);

        // 2. Chunk
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

    // Runtime Replacement \\

    // The item turned into another where it stands, once the other's shape fits there — the real item it was is gone
    public WorldItemInstance replaceItem(WorldItemInstance instance, ItemDefinitionHandle replacement) {

        ChunkInstance chunk = worldStreamManager.getChunkInstance(instance.getChunkCoordinate());
        long packedPosition = instance.getPackedPosition();

        if (chunk == null || !worldItemSpaceSystem.fits(
                chunk.getWorldHandle(),
                instance.getChunkCoordinate(),
                replacement,
                Coordinate4Long.unpackW(packedPosition),
                Coordinate4Long.unpackX(packedPosition),
                Coordinate4Long.unpackY(packedPosition),
                Coordinate4Long.unpackZ(packedPosition),
                instance))
            return null;

        removeItem(chunk, instance);
        placementScratch.set(instance.getChunkCoordinate(), packedPosition);

        return placeItem(placementScratch, itemManager.createItem(replacement));
    }

    // State \\

    // True while the item still stands in its chunk's live palette — a rebuilt palette holds new instances
    public boolean isPlaced(WorldItemInstance instance) {

        ChunkInstance chunk = worldStreamManager.getChunkInstance(instance.getChunkCoordinate());

        return chunk != null && chunk.getWorldItemInstancePaletteHandle().contains(instance);
    }

    // Open \\

    // True when the item is a container whose lid's clearance stands empty
    public boolean canOpen(WorldItemInstance instance) {
        return instance.getItemDefinitionHandle().isContainer() && worldItemSpaceSystem.isLidClear(instance);
    }

    // An open container claims its lid's clearance and is drawn open by whoever opened it
    public void setItemOpen(WorldItemInstance instance, boolean open) {

        worldItemSpaceSystem.setOpen(instance, open);

        if (open)
            worldItemRenderSystem.hideItem(instance);
        else
            worldItemRenderSystem.showItem(instance);
    }

    // Transform \\

    // T(chunk offset + position) * T(centre) * R(orientation) * T(-centre) — false unless the chunk borders this one
    public boolean composeTransform(
            WorldItemInstance instance,
            WorldHandle worldHandle,
            long chunkCoordinate,
            Matrix4 out) {

        float svr = EngineSetting.SUB_VOXEL_RESOLUTION;
        long packed = instance.getPackedPosition();

        for (int offsetZ = -1; offsetZ <= 1; offsetZ++)
            for (int offsetX = -1; offsetX <= 1; offsetX++) {

                long neighbourCoordinate = WorldWrapUtility.wrapAroundWorld(
                        worldHandle, Coordinate2Long.add(chunkCoordinate, offsetX, offsetZ));

                if (neighbourCoordinate != instance.getChunkCoordinate())
                    continue;

                itemRotationBufferSystem.composeTransform(
                        offsetX * EngineSetting.CHUNK_SIZE + Coordinate4Long.unpackX(packed) / svr,
                        Coordinate4Long.unpackY(packed) / svr,
                        offsetZ * EngineSetting.CHUNK_SIZE + Coordinate4Long.unpackZ(packed) / svr,
                        Coordinate4Long.unpackW(packed),
                        out);

                return true;
            }

        return false;
    }

    // Contents \\

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
        worldItemSpaceSystem.resolveBounds(instance);
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