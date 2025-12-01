package com.AdventureRPG.Core.RenderPipeline.RenderManager;

import java.util.List;

import com.AdventureRPG.Core.Bootstrap.SystemFrame;
import com.AdventureRPG.Core.RenderPipeline.MaterialManager.MaterialData;
import com.AdventureRPG.Core.RenderPipeline.MaterialManager.MaterialSystem;
import com.AdventureRPG.Core.RenderPipeline.RenderableInstance.MeshData;
import com.AdventureRPG.Core.RenderPipeline.RenderableInstance.MeshPacket;
import com.AdventureRPG.Core.RenderPipeline.Util.GPUCall;
import com.AdventureRPG.Core.RenderPipeline.Util.GPUHandle;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class ModelBatch extends SystemFrame {

    // Internal
    private MaterialSystem materialSystem;
    private Int2ObjectMap<GPUHandle> models;
    private Int2ObjectMap<MeshData> modelData;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.models = new Int2ObjectOpenHashMap<>();
        this.modelData = new Int2ObjectOpenHashMap<>();
    }

    @Override
    protected void init() {

        // Internal
        this.materialSystem = gameEngine.get(MaterialSystem.class);
    }

    // Add \\

    public void addModel(MeshPacket meshPacket) {
        for (List<MeshData> list : meshPacket.packet.values())
            for (MeshData meshData : list)
                addMesh(meshData);
    }

    private void addMesh(MeshData meshData) {

        int cpuHandle = meshData.handle;

        if (models.containsKey(cpuHandle))
            removeMesh(meshData);

        GPUHandle handle = GPUCall.pushData(meshData);

        models.put(cpuHandle, handle);
        modelData.put(cpuHandle, meshData);
    }

    // Remove \\

    public void removeModel(MeshPacket meshPacket) {
        for (List<MeshData> list : meshPacket.packet.values())
            for (MeshData meshData : list)
                removeMesh(meshData);
    }

    private void removeMesh(MeshData meshData) {

        int cpuHandle = meshData.handle;

        if (!models.containsKey(cpuHandle))
            return;

        GPUHandle handle = models.remove(cpuHandle);

        GPUCall.removeData(handle);

        models.remove(cpuHandle);
    }

    // Draw \\
    public void draw() {
        for (Int2ObjectMap.Entry<MeshData> entry : modelData.int2ObjectEntrySet()) {
            MeshData meshData = entry.getValue();
            MaterialData matData = materialSystem.getById(meshData.materialId);
            if (matData == null)
                continue;

            ShaderProgram shader = materialSystem.getShaderForMaterial(matData.material);
            if (shader == null)
                continue;

            shader.bind();

            // Only push per-instance uniforms (MeshPacket)
            materialSystem.pushUniforms(matData, meshData.getMeshPacket().getUniformPacket());

            // Always push transform separately if needed
            shader.setUniformMatrix("u_ModelMatrix", meshData.getTransform());

            GPUHandle handle = models.get(meshData.handle);
            if (handle == null)
                continue;

            GPUCall.bind(handle);
            GPUCall.draw(handle, meshData.getVertexCount());
            GPUCall.unbind(handle);
        }
    }
}