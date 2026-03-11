package com.internal.bootstrap.worldpipeline.worlditemrendersystem;

import com.internal.bootstrap.geometrypipeline.compositebuffer.CompositeBufferInstance;
import com.internal.bootstrap.geometrypipeline.compositebuffermanager.CompositeBufferManager;
import com.internal.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import com.internal.bootstrap.renderpipeline.rendersystem.RenderSystem;
import com.internal.bootstrap.shaderpipeline.material.MaterialInstance;
import com.internal.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import com.internal.bootstrap.worldpipeline.chunk.ChunkData;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.chunkstreammanager.ChunkStreamManager;
import com.internal.bootstrap.worldpipeline.worlditem.WorldItemCompositeInstance;
import com.internal.bootstrap.worldpipeline.worlditem.WorldItemInstance;
import com.internal.bootstrap.worldpipeline.worlditem.WorldItemInstancePaletteHandle;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.util.mathematics.Extras.Coordinate2Long;
import com.internal.core.util.mathematics.Extras.Coordinate4Long;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WorldItemRenderSystem extends ManagerPackage {

    private static final int[] INSTANCE_ATTR_SIZES = { 4, 2 };

    private ChunkStreamManager chunkStreamManager;
    private MaterialManager materialManager;
    private CompositeBufferManager compositeBufferManager;
    private RenderSystem renderSystem;

    private Int2ObjectOpenHashMap<WorldItemCompositeInstance> itemDefID2Composite;
    private LongOpenHashSet registeredChunks;

    @Override
    protected void create() {
        this.itemDefID2Composite = new Int2ObjectOpenHashMap<>();
        this.registeredChunks = new LongOpenHashSet();
    }

    @Override
    protected void get() {
        this.chunkStreamManager = get(ChunkStreamManager.class);
        this.materialManager = get(MaterialManager.class);
        this.compositeBufferManager = get(CompositeBufferManager.class);
        this.renderSystem = get(RenderSystem.class);
    }

    @Override
    protected void update() {
        Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks = chunkStreamManager.getActiveChunks();

        for (var entry : activeChunks.long2ObjectEntrySet()) {
            long coord = entry.getLongKey();
            ChunkInstance chunk = entry.getValue();
            if (registeredChunks.contains(coord))
                continue;
            if (!chunk.getChunkDataSyncContainer().hasData(ChunkData.ITEM_DATA))
                continue;
            registerChunk(chunk, coord);
        }

        var it = registeredChunks.iterator();
        while (it.hasNext()) {
            long coord = it.nextLong();
            ChunkInstance chunk = activeChunks.get(coord);
            if (chunk == null || !chunk.getChunkDataSyncContainer().hasData(ChunkData.ITEM_DATA)) {
                unregisterChunk(chunk);
                it.remove();
            }
        }

        for (var entry : itemDefID2Composite.int2ObjectEntrySet()) {
            WorldItemCompositeInstance composite = entry.getValue();
            CompositeBufferInstance buffer = composite.getCompositeBuffer();
            if (buffer.isEmpty())
                continue;
            renderSystem.pushCompositeCall(composite.getMaterial(), buffer);
        }
    }

    private void registerChunk(ChunkInstance chunk, long chunkCoordinate) {
        WorldItemInstancePaletteHandle palette = chunk.getWorldItemInstancePaletteHandle();
        ObjectArrayList<WorldItemInstance> items = palette.getItems();
        int chunkX = Coordinate2Long.unpackX(chunkCoordinate);
        int chunkZ = Coordinate2Long.unpackY(chunkCoordinate);
        for (int i = 0; i < items.size(); i++)
            addToBuffer(items.get(i), chunkX, chunkZ);
        registeredChunks.add(chunkCoordinate);
    }

    private void unregisterChunk(ChunkInstance chunk) {
        if (chunk == null)
            return;
        ObjectArrayList<WorldItemInstance> items = chunk.getWorldItemInstancePaletteHandle().getItems();
        for (int i = 0; i < items.size(); i++)
            removeItem(items.get(i));
    }

    public void addItem(WorldItemInstance instance, long chunkCoordinate) {
        int chunkX = Coordinate2Long.unpackX(chunkCoordinate);
        int chunkZ = Coordinate2Long.unpackY(chunkCoordinate);
        addToBuffer(instance, chunkX, chunkZ);
    }

    public void removeItem(WorldItemInstance instance) {
        int itemDefID = instance.getItemDefinitionHandle().getItemID();
        WorldItemCompositeInstance composite = itemDefID2Composite.get(itemDefID);
        if (composite == null)
            return;
        int slot = instance.getInstanceSlot();
        if (slot == -1)
            return;
        CompositeBufferInstance buffer = composite.getCompositeBuffer();
        int movedFromSlot = buffer.removeInstance(slot);
        instance.clearInstanceSlot();
        if (slot != movedFromSlot) {
            // Scoped to this definition's item type — slot indices are per-buffer
            WorldItemInstance moved = findInstanceBySlot(itemDefID, movedFromSlot);
            if (moved != null)
                moved.setInstanceSlot(slot);
        }
    }

    private void addToBuffer(WorldItemInstance instance, int chunkX, int chunkZ) {
        long packed = instance.getPackedPosition();
        int subX = Coordinate4Long.unpackX(packed);
        int subY = Coordinate4Long.unpackY(packed);
        int subZ = Coordinate4Long.unpackZ(packed);
        int orientation = Coordinate4Long.unpackW(packed);

        float localX = subX / 32.0f;
        float localY = subY / 32.0f;
        float localZ = subZ / 32.0f;

        WorldItemCompositeInstance composite = getOrCreateComposite(instance.getItemDefinitionHandle());
        float[] data = {
                Float.intBitsToFloat(chunkX),
                Float.intBitsToFloat(chunkZ),
                localX, localZ,
                localY, orientation
        };
        int slot = composite.getCompositeBuffer().addInstance(data);
        instance.setInstanceSlot(slot);
    }

    private WorldItemCompositeInstance getOrCreateComposite(ItemDefinitionHandle def) {
        int itemDefID = def.getItemID();
        WorldItemCompositeInstance composite = itemDefID2Composite.get(itemDefID);
        if (composite == null) {
            MaterialInstance material = materialManager.cloneMaterial(def.getMaterialID());
            CompositeBufferInstance buffer = create(CompositeBufferInstance.class);
            compositeBufferManager.constructor(buffer, def.getMeshHandle(), INSTANCE_ATTR_SIZES);
            composite = create(WorldItemCompositeInstance.class);
            composite.constructor(material, buffer);
            itemDefID2Composite.put(itemDefID, composite);
        }
        return composite;
    }

    // Scoped to a specific item definition — slot indices are per-buffer, not
    // global
    private WorldItemInstance findInstanceBySlot(int itemDefID, int slot) {
        Long2ObjectLinkedOpenHashMap<ChunkInstance> activeChunks = chunkStreamManager.getActiveChunks();
        for (long coord : registeredChunks) {
            ChunkInstance chunk = activeChunks.get(coord);
            if (chunk == null)
                continue;
            ObjectArrayList<WorldItemInstance> items = chunk.getWorldItemInstancePaletteHandle().getItems();
            for (int i = 0; i < items.size(); i++) {
                WorldItemInstance item = items.get(i);
                if (item.getItemDefinitionHandle().getItemID() == itemDefID
                        && item.getInstanceSlot() == slot)
                    return item;
            }
        }
        return null;
    }
}