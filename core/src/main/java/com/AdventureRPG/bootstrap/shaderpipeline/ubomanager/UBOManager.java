package com.AdventureRPG.bootstrap.shaderpipeline.ubomanager;

import com.AdventureRPG.bootstrap.shaderpipeline.uniforms.Uniform;
import com.AdventureRPG.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.AdventureRPG.core.engine.ManagerPackage;

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
        this.internalBuildSystem = create(InternalBuildSystem.class);

        this.nextAvailableBinding = 0;
        this.releasedBindings = new IntOpenHashSet();

        // Retrieval Mapping
        this.uboName2UBOHandle = new Object2ObjectOpenHashMap<>();
    }

    @Override
    protected void release() {
        this.internalBuildSystem = release(InternalBuildSystem.class);
    }

    @Override
    protected void dispose() {
        for (UBOHandle handle : uboName2UBOHandle.values())
            GLSLUtility.deleteUniformBuffer(handle.getGpuHandle());

        uboName2UBOHandle.clear();
        releasedBindings.clear();
    }

    // UBO Management \\

    public UBOHandle buildBuffer(UBOData data) {

        String blockName = data.getBlockName();

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
        trackBindingPoint(handle.getBindingPoint());

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
        int newBinding = allocateBindingPoint();
        int gpuHandle = GLSLUtility.createUniformBuffer();

        // NEW: pass total size from source
        UBOHandle clone = create(UBOHandle.class);
        clone.constructor(
                source.getBufferName(),
                0,
                gpuHandle,
                newBinding,
                source.getTotalSizeBytes()); // NEW: get from source

        // ... rest of cloning logic ...

        GLSLUtility.allocateUniformBuffer(gpuHandle, source.getTotalSizeBytes());
        GLSLUtility.bindUniformBufferBase(gpuHandle, newBinding);

        return clone;
    }

    public void destroyUBO(UBOHandle handle) {

        // Remove from tracking
        uboName2UBOHandle.remove(handle.getBufferName());

        // Delete GPU resource
        GLSLUtility.deleteUniformBuffer(handle.getGpuHandle());

        // Release the binding point for reuse
        releaseBindingPoint(handle.getBindingPoint());
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