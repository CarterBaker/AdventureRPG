package com.AdventureRPG.core.shaders.ubomanager;

import com.AdventureRPG.core.engine.ManagerFrame;
import com.AdventureRPG.core.shaders.uniforms.Uniform;
import com.AdventureRPG.core.shaders.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Exceptions.GraphicException;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public final class UBOManager extends ManagerFrame {

    // Internal
    private InternalBuildSystem internalBuildSystem;

    // Retrieval Mapping
    private Object2ObjectOpenHashMap<String, UBOHandle> uboName2UBOHandle;

    @Override
    protected void create() {
        this.internalBuildSystem = (InternalBuildSystem) register(new InternalBuildSystem());
        this.uboName2UBOHandle = new Object2ObjectOpenHashMap<>();
    }

    @Override
    protected void freeMemory() {
        this.internalBuildSystem = (InternalBuildSystem) release(internalBuildSystem);
    }

    @Override
    protected void dispose() {
        for (UBOHandle handle : uboName2UBOHandle.values())
            GLSLUtility.deleteUniformBuffer(handle.gpuHandle);

        uboName2UBOHandle.clear();
    }

    // UBO Management \\

    public UBOHandle buildBuffer(UBOData data) {

        String blockName = data.blockName();

        // Check if buffer already exists
        UBOHandle existing = uboName2UBOHandle.get(blockName);

        if (existing != null) {

            // Validate it matches the existing structure
            internalBuildSystem.validate(existing, data);
            return existing; // Reuse existing handle
        }

        // Build new handle from data
        UBOHandle handle = internalBuildSystem.build(data);
        uboName2UBOHandle.put(blockName, handle);

        return handle;
    }

    public UBOHandle getUBOHandleFromUBOName(String blockName) {

        UBOHandle handle = uboName2UBOHandle.get(blockName);

        if (handle == null)
            throw new GraphicException.ShaderProgramException(
                    "UBO not found: " + blockName);

        return handle;
    }

    public UBOHandle cloneUBOHandle(UBOHandle source) {

        // Create new GPU buffer
        int gpuHandle = GLSLUtility.createUniformBuffer();

        // Create new handle with same structure
        UBOHandle clone = new UBOHandle(
                source.bufferName,
                0,
                gpuHandle,
                source.bindingPoint);

        // Clone all uniforms with their attributes
        Object2ObjectOpenHashMap<String, Uniform<?>> sourceUniforms = source.getUniforms();

        for (var entry : sourceUniforms.entrySet()) {
            String uniformName = entry.getKey();
            Uniform<?> sourceUniform = entry.getValue();

            // Create new uniform attribute instance by calling the builder
            UniformAttribute<?> clonedAttribute = internalBuildSystem
                    .createUniformAttributeClone(sourceUniform.attribute());

            // Create new uniform with same offset
            Uniform<?> clonedUniform = new Uniform<>(
                    -1, // No handle for UBO uniforms
                    sourceUniform.offset,
                    clonedAttribute);

            clone.addUniform(uniformName, clonedUniform);
        }

        // Calculate total size from source uniforms
        int totalSize = calculateTotalSize(sourceUniforms);

        // Allocate GPU buffer with computed size
        GLSLUtility.allocateUniformBuffer(gpuHandle, totalSize);

        // Bind to same binding point
        GLSLUtility.bindUniformBufferBase(gpuHandle, source.bindingPoint);

        return clone;
    }

    private int calculateTotalSize(Object2ObjectOpenHashMap<String, Uniform<?>> uniforms) {
        int maxEnd = 0;

        for (Uniform<?> uniform : uniforms.values()) {
            int end = uniform.offset + getAttributeSize(uniform.attribute());
            if (end > maxEnd)
                maxEnd = end;
        }

        // Align to 16 bytes (std140 requirement)
        return ((maxEnd + 15) / 16) * 16;
    }

    private int getAttributeSize(UniformAttribute<?> attribute) {
        // Return approximate size - this is a fallback
        // The actual size should be calculated properly based on type
        return attribute.getByteBuffer().capacity();
    }
}