package com.internal.bootstrap.shaderpipeline.ubomanager;

import java.io.File;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.internal.bootstrap.shaderpipeline.ubo.UBOInstance;
import com.internal.bootstrap.shaderpipeline.uniforms.Uniform;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformData;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformType;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformUtility;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.util.JsonUtility;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/*
 * Single authority on UBO lifetime and binding point allocation.
 * Handles are owned here and never leave this system — external callers
 * receive a UBOInstance via cloneUBO(). Both shader-sourced and JSON-sourced
 * UBO registrations go through buildBuffer(), which ensures the binding registry
 * is never split across systems.
 *
 * Bootstrap: InternalLoadManager scans UBO_JSON_PATH, InternalBuildSystem parses
 * each file into UBOData, and buildBuffer() constructs and registers the handle.
 * Both self-release when the queue empties. On-demand loading is available via
 * getUBOHandleFromUBOName() while the loader is still alive.
 *
 * findUBOHandle() provides a soft lookup that returns null instead of throwing —
 * used by systems (e.g. TextureManager) that treat a missing UBO as optional.
 */
public class UBOManager extends ManagerPackage {

    // Internal
    private int nextID;
    private int nextAvailableBinding;
    private IntOpenHashSet usedBindings;
    private IntOpenHashSet releasedBindings;

    // Retrieval Mapping
    private Object2ObjectOpenHashMap<String, UBOHandle> uboName2UBOHandle;

    // Internal \\

    @Override
    protected void create() {
        create(InternalLoadManager.class);

        this.nextID = 0;
        this.nextAvailableBinding = 0;
        this.usedBindings = new IntOpenHashSet();
        this.releasedBindings = new IntOpenHashSet();

        this.uboName2UBOHandle = new Object2ObjectOpenHashMap<>();
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

    // On-Demand Loading \\

    public void request(String blockName) {
        ((InternalLoadManager) internalLoader).request(blockName);
    }

    // UBO Management \\

    public UBOHandle buildBuffer(UBOData data) {

        String blockName = data.getBlockName();
        UBOHandle existing = uboName2UBOHandle.get(blockName);

        if (existing != null) {
            validate(existing, data);
            return existing;
        }

        int binding = resolveBinding(data);
        int id = nextID++;
        int gpuHandle = GLSLUtility.createUniformBuffer();
        int totalSize = computeStd140BufferSize(data.getUniforms());

        UBOHandle handle = create(UBOHandle.class);
        handle.constructor(blockName, id, gpuHandle, binding, totalSize);

        populateUniforms(handle, data.getUniforms());

        GLSLUtility.allocateUniformBuffer(gpuHandle, totalSize);
        GLSLUtility.bindUniformBufferBase(gpuHandle, binding);

        uboName2UBOHandle.put(blockName, handle);

        return handle;
    }

    public UBOInstance cloneUBO(UBOHandle handle) {

        if (handle == null)
            throwException("Cannot clone UBO — handle is null");

        int newGpuHandle = GLSLUtility.createUniformBuffer();
        GLSLUtility.allocateUniformBuffer(newGpuHandle, handle.getTotalSizeBytes());
        GLSLUtility.bindUniformBufferBase(newGpuHandle, handle.getBindingPoint());

        UBOInstance instance = create(UBOInstance.class);
        instance.constructor(
                handle.getBufferName(),
                newGpuHandle,
                handle.getBindingPoint(),
                handle.getTotalSizeBytes());

        Object2ObjectOpenHashMap<String, Uniform<?>> sourceUniforms = handle.getUniforms();
        String[] keys = sourceUniforms.keySet().toArray(new String[0]);

        for (int i = 0; i < keys.length; i++) {
            Uniform<?> source = sourceUniforms.get(keys[i]);
            UniformAttribute<?> newAttr = source.attribute().createDefault();
            instance.addUniform(keys[i], new Uniform<>(-1, source.offset, newAttr));
        }

        return instance;
    }

    public void destroyInstance(UBOInstance instance) {
        GLSLUtility.deleteUniformBuffer(instance.getGpuHandle());
    }

    /*
     * Hard lookup — triggers an on-demand load if not yet registered.
     * Throws if the block name is not found in the scan registry.
     * Use this when the UBO is required.
     */
    public UBOHandle getUBOHandleFromUBOName(String blockName) {

        if (!uboName2UBOHandle.containsKey(blockName))
            request(blockName);

        return uboName2UBOHandle.get(blockName);
    }

    /*
     * Soft lookup — returns null if the handle is not registered and the
     * block name is not found in the scan registry.
     * Use this when the UBO is optional (e.g. texture array seeding).
     */
    public UBOHandle findUBOHandle(String blockName) {

        UBOHandle handle = uboName2UBOHandle.get(blockName);

        if (handle != null)
            return handle;

        try {
            request(blockName);
        } catch (Exception e) {
            return null;
        }

        return uboName2UBOHandle.get(blockName);
    }

    // Validate \\

    private void validate(UBOHandle existing, UBOData newData) {

        if (newData.getBinding() != UBOData.UNSPECIFIED_BINDING &&
                existing.getBindingPoint() != newData.getBinding())
            throwException(
                    "UBO '" + newData.getBlockName() + "' has conflicting binding: " +
                            "existing=" + existing.getBindingPoint() +
                            ", requested=" + newData.getBinding() +
                            ". All declarations of this block must use the same binding point.");

        ObjectArrayList<UniformData> newUniforms = newData.getUniforms();
        Object2ObjectOpenHashMap<String, Uniform<?>> existingUniforms = existing.getUniforms();

        if (newUniforms.size() != existingUniforms.size())
            throwException(
                    "UBO '" + newData.getBlockName() + "' has conflicting structure: " +
                            existingUniforms.size() + " vs " + newUniforms.size() + " uniforms. " +
                            "Use an #include to ensure consistent block definitions across shaders.");

        for (int i = 0; i < newUniforms.size(); i++) {
            if (existingUniforms.get(newUniforms.get(i).getUniformName()) == null)
                throwException(
                        "UBO '" + newData.getBlockName() + "' has conflicting structure: uniform '" +
                                newUniforms.get(i).getUniformName() +
                                "' not found in existing definition. " +
                                "Use an #include to ensure consistent block definitions across shaders.");
        }
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

    // Std140 Layout \\

    private int computeStd140BufferSize(ObjectArrayList<UniformData> uniformsData) {

        int offset = 0;

        for (int i = 0; i < uniformsData.size(); i++) {
            UniformData ud = uniformsData.get(i);
            offset = UniformUtility.align(offset, UniformUtility.getStd140Alignment(ud));
            offset += UniformUtility.getStd140Size(ud);
        }

        return UniformUtility.align(offset, 16);
    }

    private void populateUniforms(UBOHandle handle, ObjectArrayList<UniformData> uniformsData) {

        int offset = 0;

        for (int i = 0; i < uniformsData.size(); i++) {
            UniformData ud = uniformsData.get(i);
            offset = UniformUtility.align(offset, UniformUtility.getStd140Alignment(ud));

            handle.addUniform(
                    ud.getUniformName(),
                    new Uniform<>(-1, offset, UniformUtility.createUniformAttribute(ud)));

            offset += UniformUtility.getStd140Size(ud);
        }
    }
}