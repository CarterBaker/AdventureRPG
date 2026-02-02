package com.internal.bootstrap.shaderpipeline.ubomanager;

import com.internal.bootstrap.shaderpipeline.uniforms.Uniform;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.internal.core.engine.ManagerPackage;

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

    public UBOHandle cloneUBO(UBOHandle source) {

        // REUSE the source's binding point
        int sharedBinding = source.getBindingPoint();
        int newGpuHandle = GLSLUtility.createUniformBuffer();

        UBOHandle copy = create(UBOHandle.class);
        copy.constructor(
                source.getBufferName(),
                source.getBufferID(),
                newGpuHandle,
                sharedBinding,
                source.getTotalSizeBytes());

        // Copy uniform structure
        for (String uniformName : source.getUniforms().keySet()) {
            Uniform<?> sourceUniform = source.getUniform(uniformName);
            UniformAttribute<?> newAttribute = sourceUniform.attribute.createDefault();
            Uniform<?> copiedUniform = createUniform(sourceUniform.offset, newAttribute);
            copy.addUniform(uniformName, copiedUniform);
        }

        // Allocate GPU buffer
        GLSLUtility.allocateUniformBuffer(newGpuHandle, source.getTotalSizeBytes());

        return copy;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Uniform<?> createUniform(int offset, UniformAttribute<?> attribute) {
        return new Uniform(0, offset, attribute);
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
}