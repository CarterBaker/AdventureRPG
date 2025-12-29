package com.AdventureRPG.core.shaderpipeline.ubomanager;

import com.AdventureRPG.core.engine.ManagerPackage;
import com.AdventureRPG.core.shaderpipeline.uniforms.Uniform;
import com.AdventureRPG.core.shaderpipeline.uniforms.UniformAttribute;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public final class UBOManager extends ManagerPackage {

    // Internal
    private InternalBuildSystem internalBuildSystem;

    private int nextAvailableBinding;
    private IntOpenHashSet releasedBindings;

    // Retrieval Mapping
    private Object2ObjectOpenHashMap<String, UBOHandle> uboName2UBOHandle;

    @Override
    protected void create() {

        // Internal
        this.internalBuildSystem = (InternalBuildSystem) register(new InternalBuildSystem());

        this.nextAvailableBinding = 0;
        this.releasedBindings = new IntOpenHashSet();

        // Retrieval Mapping
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
        releasedBindings.clear();
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

        // Track the binding point
        trackBindingPoint(handle.bindingPoint);

        return handle;
    }

    public UBOHandle getUBOHandleFromUBOName(String blockName) {

        UBOHandle handle = uboName2UBOHandle.get(blockName);

        if (handle == null)
            throwException(
                    "UBO not found: " + blockName);

        return handle;
    }

    public UBOHandle cloneUBOHandle(UBOHandle source) {

        // Allocate a new binding point for the clone
        int newBinding = allocateBindingPoint();

        // Create new GPU buffer
        int gpuHandle = GLSLUtility.createUniformBuffer();

        // Create new handle with same structure but different binding
        UBOHandle clone = new UBOHandle(
                source.bufferName,
                0,
                gpuHandle,
                newBinding);

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

        // Bind to NEW binding point
        GLSLUtility.bindUniformBufferBase(gpuHandle, newBinding);

        return clone;
    }

    public void destroyUBO(UBOHandle handle) {

        // Remove from tracking
        uboName2UBOHandle.remove(handle.bufferName);

        // Delete GPU resource
        GLSLUtility.deleteUniformBuffer(handle.gpuHandle);

        // Release the binding point for reuse
        releaseBindingPoint(handle.bindingPoint);
    }

    // Binding Point Management \\

    private void trackBindingPoint(int binding) {
        // If this is the current "next" binding, advance the counter
        if (binding == nextAvailableBinding) {
            nextAvailableBinding++;
        }
        // Otherwise it was explicitly specified or reused - no action needed
    }

    private int allocateBindingPoint() {
        // First, check if we have any released bindings to reuse
        if (!releasedBindings.isEmpty()) {
            int reusedBinding = releasedBindings.iterator().nextInt();
            releasedBindings.remove(reusedBinding);
            return reusedBinding;
        }

        // Otherwise, use the next available binding and increment
        return nextAvailableBinding++;
    }

    private void releaseBindingPoint(int binding) {
        // Add to the pool of reusable bindings
        releasedBindings.add(binding);
    }

    // Utility \\

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