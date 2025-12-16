package com.AdventureRPG.core.shaderpipeline.UBOManager;

import com.AdventureRPG.core.kernel.ManagerFrame;
import com.AdventureRPG.core.util.Exceptions.GraphicException;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public final class UBOManager extends ManagerFrame {

    // Internal
    private InternalBuildSystem internalBuildSystem;

    // Retrieval Mapping
    private Object2ObjectOpenHashMap<String, UBOHandle> buffers;

    @Override
    protected void create() {
        this.internalBuildSystem = (InternalBuildSystem) register(new InternalBuildSystem());
        this.buffers = new Object2ObjectOpenHashMap<>();
    }

    @Override
    protected void freeMemory() {
        this.internalBuildSystem = (InternalBuildSystem) release(internalBuildSystem);
    }

    @Override
    protected void dispose() {
        for (UBOHandle handle : buffers.values())
            GLSLUtility.deleteUniformBuffer(handle.gpuHandle);

        buffers.clear();
    }

    // UBO Management \\

    public UBOHandle registerBuffer(UBOData data) {

        // Check if buffer already exists
        UBOHandle existing = buffers.get(data.blockName());

        if (existing != null) {
            // Validate it matches
            internalBuildSystem.validate(existing, data);
            return existing; // Reuse
        }

        // Build new
        UBOHandle handle = internalBuildSystem.build(data);
        buffers.put(data.blockName(), handle);

        return handle;
    }

    public void update(String bufferName, String uniformName, Object value) {

        UBOHandle handle = buffers.get(bufferName);

        if (handle == null)
            throw new GraphicException.ShaderProgramException(
                    "UBO not found: " + bufferName);

        handle.update(uniformName, value);
    }
}