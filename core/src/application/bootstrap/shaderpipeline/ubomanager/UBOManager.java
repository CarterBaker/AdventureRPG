package application.bootstrap.shaderpipeline.ubomanager;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.nio.ByteBuffer;

import application.bootstrap.shaderpipeline.ubo.UBOData;
import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import application.bootstrap.shaderpipeline.ubo.UBOInstance;
import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformData;
import application.bootstrap.shaderpipeline.uniforms.UniformStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformUtility;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;

public class UBOManager extends ManagerPackage {

    /*
     * Single authority on UBO lifetime, binding point allocation, and all GPU
     * operations. Owns all UBOHandles permanently. External callers receive a
     * UBOInstance via createUBOInstance() and push updates via push(). Handle
     * and Instance never touch GL directly. UBO IDs are assigned in
     * registration order.
     */

    // Internal
    private int nextAvailableBinding;
    private IntOpenHashSet usedBindings;

    // Palette
    private Object2IntOpenHashMap<String> uboName2UBOID;
    private ObjectArrayList<UBOHandle> uboID2UBOHandle;
    private ObjectArrayList<UBOInstance> activeInstances;

    // Base \\

    @Override
    protected void create() {

        this.nextAvailableBinding = 0;
        this.usedBindings = new IntOpenHashSet();
        this.uboName2UBOID = RegistryUtility.createNameIndex();
        this.uboID2UBOHandle = RegistryUtility.createPalette();
        this.activeInstances = new ObjectArrayList<>();

        create(UBOLoader.class);
    }

    @Override
    protected void dispose() {

        for (int uboID = 0; uboID < uboID2UBOHandle.size(); uboID++) {

            UBOHandle handle = uboID2UBOHandle.get(uboID);

            if (handle != null)
                UBOGLSLUtility.deleteUniformBuffer(handle.getGpuHandle());
        }

        for (int i = 0; i < activeInstances.size(); i++)
            UBOGLSLUtility.deleteUniformBuffer(activeInstances.get(i).getGpuHandle());

        RegistryUtility.clearPalette(uboName2UBOID, uboID2UBOHandle);
        activeInstances.clear();
        usedBindings.clear();
    }

    // Management \\

    void buildBuffer(UBOHandle handle) {

        String blockName = handle.getBlockName();

        if (hasUBO(blockName))
            return;

        int binding = resolveBinding(handle.getRequestedBinding(), blockName);
        int id = RegistryUtility.registerID(
                uboName2UBOID, uboID2UBOHandle, blockName, EngineSetting.REGISTRY_INT_ID_COUNT);
        int gpuHandle = UBOGLSLUtility.createUniformBuffer();
        int totalSize = computeStd140BufferSize(handle.getUniformDeclarations());

        handle.initRuntime(id, gpuHandle, binding, totalSize);

        populateUniforms(handle);

        UBOGLSLUtility.allocateUniformBuffer(gpuHandle, totalSize);
        UBOGLSLUtility.bindUniformBufferBase(gpuHandle, binding);

        uboID2UBOHandle.set(id, handle);
    }

    public UBOInstance createUBOInstance(UBOHandle handle) {

        if (handle == null)
            throwException("Cannot create UBO instance — handle is null");

        int newGpuHandle = UBOGLSLUtility.createUniformBuffer();
        UBOGLSLUtility.allocateUniformBuffer(newGpuHandle, handle.getTotalSizeBytes());
        UBOGLSLUtility.bindUniformBufferBase(newGpuHandle, handle.getBindingPoint());

        UBOData clonedData = new UBOData(handle.getUBOData(), newGpuHandle);
        UBOInstance instance = create(UBOInstance.class);
        instance.constructor(clonedData);
        activeInstances.add(instance);

        Object2ObjectOpenHashMap<String, UniformStruct<?>> sourceUniforms = handle.getCompiledUniforms();
        ObjectArrayList<String> uniformKeys = handle.getUniformKeys();

        for (int i = 0; i < uniformKeys.size(); i++) {
            String key = uniformKeys.get(i);
            UniformStruct<?> source = sourceUniforms.get(key);
            UniformAttributeStruct<?> newAttr = source.attribute().createDefault();
            instance.addCompiledUniform(key, new UniformStruct<>(-1, source.getOffset(), newAttr));
        }

        return instance;
    }

    public void destroyInstance(UBOInstance instance) {
        activeInstances.remove(instance);
        UBOGLSLUtility.deleteUniformBuffer(instance.getGpuHandle());
    }

    public void push(UBOHandle handle) {
        ByteBuffer staging = handle.getUBOData().getStagingBuffer();
        staging.rewind();
        UBOGLSLUtility.updateUniformBuffer(handle.getGpuHandle(), 0, staging);
    }

    public void push(UBOInstance instance) {
        ByteBuffer staging = instance.getUBOData().getStagingBuffer();
        staging.rewind();
        UBOGLSLUtility.updateUniformBuffer(instance.getGpuHandle(), 0, staging);
    }

    public void bindBuffersForCurrentContext() {

        // Shared/source UBO handles may not be re-bound every draw call.
        // Re-assert their binding points whenever a context is made current.
        for (int uboID = 0; uboID < uboID2UBOHandle.size(); uboID++) {

            UBOHandle handle = uboID2UBOHandle.get(uboID);

            if (handle != null)
                UBOGLSLUtility.bindUniformBufferBase(handle.getGpuHandle(), handle.getBindingPoint());
        }
    }

    // Binding Registry \\

    private int resolveBinding(int requestedBinding, String blockName) {

        if (requestedBinding == UBOData.UNSPECIFIED_BINDING)
            return allocateBindingPoint();

        if (usedBindings.contains(requestedBinding))
            throwException(
                    "Binding point collision: UBO '" + blockName +
                            "' requested binding " + requestedBinding + " which is already in use.");

        usedBindings.add(requestedBinding);

        if (requestedBinding >= nextAvailableBinding)
            nextAvailableBinding = requestedBinding + 1;

        return requestedBinding;
    }

    private int allocateBindingPoint() {

        int binding = nextAvailableBinding++;

        usedBindings.add(binding);
        return binding;
    }

    // Std140 Layout \\

    private int computeStd140BufferSize(ObjectArrayList<UniformData> uniformDeclarations) {

        int offset = 0;

        for (int i = 0; i < uniformDeclarations.size(); i++) {
            UniformData ud = uniformDeclarations.get(i);
            offset = UniformUtility.align(offset, UniformUtility.getStd140Alignment(ud));
            offset += UniformUtility.getStd140Size(ud);
        }

        return UniformUtility.align(offset, 16);
    }

    private void populateUniforms(UBOHandle handle) {

        int offset = 0;
        ObjectArrayList<UniformData> declarations = handle.getUniformDeclarations();

        for (int i = 0; i < declarations.size(); i++) {
            UniformData ud = declarations.get(i);
            offset = UniformUtility.align(offset, UniformUtility.getStd140Alignment(ud));
            handle.addCompiledUniform(
                    ud.getUniformName(),
                    new UniformStruct<>(-1, offset, UniformUtility.createUniformAttribute(ud)));
            offset += UniformUtility.getStd140Size(ud);
        }
    }

    // Accessible \\

    public void request(String blockName) {
        ((UBOLoader) internalLoader).request(blockName);
    }

    public boolean hasUBO(String uboName) {
        return RegistryUtility.getHandle(uboName2UBOID, uboID2UBOHandle, uboName) != null;
    }

    public int getUBOIDFromUBOName(String uboName) {

        if (!hasUBO(uboName))
            request(uboName);

        return uboName2UBOID.getInt(uboName);
    }

    public UBOHandle getUBOHandleFromUBOID(int uboID) {

        UBOHandle handle = RegistryUtility.getHandle(uboID2UBOHandle, uboID);

        if (handle == null)
            throwException("UBO ID not found: " + uboID);

        return handle;
    }

    public UBOHandle getUBOHandleFromUBOName(String uboName) {
        return getUBOHandleFromUBOID(getUBOIDFromUBOName(uboName));
    }

    public UBOHandle findUBOHandle(String blockName) {

        if (hasUBO(blockName))
            return RegistryUtility.getHandle(uboName2UBOID, uboID2UBOHandle, blockName);

        try {
            request(blockName);
        } catch (Exception e) {
            return null;
        }

        return RegistryUtility.getHandle(uboName2UBOID, uboID2UBOHandle, blockName);
    }
}