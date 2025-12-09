package com.AdventureRPG.core.geometrypipeline.modelmanager;

import com.AdventureRPG.core.kernel.InstanceFrame;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class ModelBatchHandle extends InstanceFrame {

    // Internal
    final Int2ObjectOpenHashMap<IntArrayList> materialID2MeshHandle;

    ModelBatchHandle(int modelDataID) {

        // Internal
        this.materialID2MeshHandle = new Int2ObjectOpenHashMap<>();
    }
}
