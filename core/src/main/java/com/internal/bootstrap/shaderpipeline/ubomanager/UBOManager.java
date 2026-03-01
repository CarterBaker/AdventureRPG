package com.internal.bootstrap.shaderpipeline.ubomanager;

import com.internal.bootstrap.shaderpipeline.ubo.UBOInstance;
import com.internal.core.engine.ManagerPackage;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

/*
 * Single authority on UBO lifetime and binding point allocation.
 * Handles are owned here and never leave this system — external callers
 * receive a UBOInstance via cloneUBO(). Both shader-sourced and JSON-sourced
 * UBO registrations go through buildBuffer(), which ensures the binding registry
 * is never split across systems.
 */
public class UBOManager extends ManagerPackage {

    // Internal
    private InternalBuildSystem internalBuildSystem;

    private int nextAvailableBinding;
    private IntOpenHashSet usedBindings;
    private IntOpenHashSet releasedBindings;

    // Retrieval Mapping
    private Object2ObjectOpenHashMap<String, UBOHandle> uboName2UBOHandle;

    // Internal \\

    @Override
    protected void create() {
        this.internalBuildSystem = create(InternalBuildSystem.class);

        this.nextAvailableBinding = 0;
        this.usedBindings = new IntOpenHashSet();
        this.releasedBindings = new IntOpenHashSet();

        this.uboName2UBOHandle = new Object2ObjectOpenHashMap<>();
    }

    @Override
    protected void release() {
        this.internalBuildSystem = release(InternalBuildSystem.class);
    }

    @Override
    protected void dispose() {

        UBOHandle[] handles = uboName2UBOHandle.values().toArray(new UBOHandle[0]);

        for (int i = 0; i < handles.length; i++)
            GLSLUtility.deleteUniformBuffer(handles[i].getGpuHandle());

        uboName2UBOHandle.clear();
        usedBindings.clear();
        releasedBindings.clear();
    }

    // UBO Management \\

    public UBOHandle buildBuffer(UBOData data) {

        String blockName = data.getBlockName();
        UBOHandle existing = uboName2UBOHandle.get(blockName);

        if (existing != null) {
            internalBuildSystem.validate(existing, data);
            return existing;
        }

        int resolvedBinding = resolveBinding(data);

        UBOHandle handle = internalBuildSystem.build(data, resolvedBinding);
        uboName2UBOHandle.put(blockName, handle);

        return handle;
    }

    public UBOInstance cloneUBO(UBOHandle handle) {

        if (handle == null)
            throwException("Cannot clone UBO — handle is null");

        return internalBuildSystem.cloneFromHandle(handle);
    }

    public void destroyInstance(UBOInstance instance) {
        GLSLUtility.deleteUniformBuffer(instance.getGpuHandle());
    }

    public UBOHandle getUBOHandleFromUBOName(String blockName) {

        UBOHandle handle = uboName2UBOHandle.get(blockName);

        if (handle == null)
            throwException("UBO not found: " + blockName);

        return handle;
    }

    // Binding Registry \\

    private int resolveBinding(UBOData data) {

        int requested = data.getBinding();

        if (requested == UBOData.UNSPECIFIED_BINDING)
            return allocateBindingPoint();

        if (usedBindings.contains(requested))
            throwException(
                    "Binding point collision: UBO '" + data.getBlockName() +
                            "' requested binding " + requested +
                            " which is already in use.");

        usedBindings.add(requested);

        // Keep the auto-counter ahead of any explicit bindings so future
        // auto-assignments never collide with manually declared ones.
        if (requested >= nextAvailableBinding)
            nextAvailableBinding = requested + 1;

        return requested;
    }

    private int allocateBindingPoint() {

        int binding;

        if (!releasedBindings.isEmpty()) {
            binding = releasedBindings.iterator().nextInt();
            releasedBindings.remove(binding);
        } else {
            binding = nextAvailableBinding++;
        }

        usedBindings.add(binding);
        return binding;
    }

    private void releaseBindingPoint(int binding) {
        usedBindings.remove(binding);
        releasedBindings.add(binding);
    }
}