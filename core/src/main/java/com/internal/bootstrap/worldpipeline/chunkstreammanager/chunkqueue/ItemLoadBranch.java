package com.internal.bootstrap.worldpipeline.chunkstreammanager.chunkqueue;

import com.internal.bootstrap.geometrypipeline.model.ModelInstance;
import com.internal.bootstrap.geometrypipeline.modelmanager.ModelManager;
import com.internal.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import com.internal.bootstrap.itempipeline.itemdefinitionmanager.ItemDefinitionManager;
import com.internal.bootstrap.shaderpipeline.material.MaterialInstance;
import com.internal.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import com.internal.bootstrap.worldpipeline.chunk.ChunkData;
import com.internal.bootstrap.worldpipeline.chunk.ChunkDataSyncContainer;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import com.internal.bootstrap.worldpipeline.worlditem.WorldItemInstance;
import com.internal.bootstrap.worldpipeline.worlditem.WorldItemInstancePaletteHandle;
import com.internal.bootstrap.worldpipeline.worlditem.WorldItemPaletteHandle;
import com.internal.bootstrap.worldpipeline.worlditem.WorldItemStruct;
import com.internal.core.engine.BranchPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.kernel.SyncConsumer.SyncStructConsumer;
import com.internal.core.kernel.thread.ThreadHandle;
import com.internal.core.util.mathematics.Extras.Coordinate3Int;
import com.internal.core.util.mathematics.Extras.Coordinate4Long;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ItemLoadBranch extends BranchPackage {

    // Internal
    private ThreadHandle threadHandle;
    private ItemDefinitionManager itemDefinitionManager;
    private MaterialManager materialManager;
    private ModelManager modelManager;
    private int itemDataIndex;

    // Internal \\

    @Override
    protected void get() {
        this.threadHandle = getThreadHandleFromThreadName("WorldStreaming");
        this.itemDefinitionManager = get(ItemDefinitionManager.class);
        this.materialManager = get(MaterialManager.class);
        this.modelManager = get(ModelManager.class);
        this.itemDataIndex = ChunkData.ITEM_DATA.index;
    }

    // Item Load \\

    public void loadItems(ChunkInstance chunkInstance) {
        executeAsync(
                threadHandle,
                chunkInstance.getChunkDataSyncContainer(),
                (SyncStructConsumer<ChunkDataSyncContainer>) container -> {

                    WorldItemInstancePaletteHandle palette = chunkInstance.getWorldItemInstancePaletteHandle();
                    palette.clear();

                    SubChunkInstance[] subChunks = chunkInstance.getSubChunks();

                    for (int i = 0; i < EngineSetting.WORLD_HEIGHT; i++) {
                        SubChunkInstance subChunk = subChunks[i];
                        WorldItemPaletteHandle structPalette = subChunk.getWorldItemPaletteHandle();

                        if (structPalette.isEmpty())
                            continue;

                        ObjectArrayList<WorldItemStruct> structs = structPalette.getItems();

                        for (int j = 0; j < structs.size(); j++) {
                            WorldItemStruct struct = structs.get(j);
                            WorldItemInstance instance = buildInstance(struct);
                            if (instance != null)
                                palette.addItem(instance);
                        }
                    }

                    container.data[itemDataIndex] = true;
                });
    }

    // Build \\

    private WorldItemInstance buildInstance(WorldItemStruct struct) {

        int itemID = struct.packedItem & 0xFFF;
        ItemDefinitionHandle def = itemDefinitionManager.getItemFromItemID(itemID);
        if (def == null)
            return null;

        int subX = Coordinate4Long.unpackX(struct.packedPosition);
        int subY = Coordinate4Long.unpackY(struct.packedPosition);
        int subZ = Coordinate4Long.unpackZ(struct.packedPosition);
        int rotation = Coordinate4Long.unpackW(struct.packedPosition);

        int blockX = subX >> 5;
        int blockY = subY >> 5;
        int blockZ = subZ >> 5;
        int packedBlockCoordinate = Coordinate3Int.pack(blockX, blockY, blockZ);

        int materialID = def.getMaterialID();
        MaterialInstance material = materialManager.cloneMaterial(materialID);

        ModelInstance model = modelManager.createModel(
                def.getMeshHandle().getMeshStruct(),
                material);

        WorldItemInstance instance = create(WorldItemInstance.class);
        instance.constructor(def, model, packedBlockCoordinate,
                struct.packedPosition, struct.packedItem);

        return instance;
    }
}