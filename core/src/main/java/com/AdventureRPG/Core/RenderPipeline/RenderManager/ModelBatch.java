package com.AdventureRPG.Core.RenderPipeline.RenderManager;

import java.util.List;

import com.AdventureRPG.Core.RenderPipeline.RenderableInstance.MeshData;
import com.AdventureRPG.Core.RenderPipeline.RenderableInstance.MeshPacket;
import com.AdventureRPG.Core.RenderPipeline.Util.GPUCall;
import com.AdventureRPG.Core.RenderPipeline.Util.GPUHandle;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class ModelBatch extends RenderContext {

    // Internal
    private final Int2ObjectMap<GPUHandle> models;
    private final Int2ObjectMap<MeshData> modelData;

    // Base \\

    public ModelBatch() {

        // Internal
        this.models = new Int2ObjectOpenHashMap<>();
        this.modelData = new Int2ObjectOpenHashMap<>();
    }

    // Add \\

    public void addModel(MeshPacket meshPacket) {
        for (List<MeshData> list : meshPacket.packet.values())
            for (MeshData meshData : list)
                addModel(meshData);
    }

    private void addModel(MeshData meshData) {

        int cpuHandle = meshData.handle;

        if (models.containsKey(cpuHandle))
            removeModel(meshData);

        GPUHandle handle = GPUCall.pushData(meshData);

        models.put(cpuHandle, handle);
        modelData.put(cpuHandle, meshData);
    }

    // Remove \\

    public void removeModel(MeshPacket meshPacket) {
        for (List<MeshData> list : meshPacket.packet.values())
            for (MeshData meshData : list)
                removeModel(meshData);
    }

    private void removeModel(MeshData meshData) {

        int cpuHandle = meshData.handle;

        if (!models.containsKey(cpuHandle))
            return;

        GPUHandle handle = models.remove(cpuHandle);

        GPUCall.removeData(handle);

        models.remove(cpuHandle);
    }

    // Draw \\

    public void draw() {

    }
}
