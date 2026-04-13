package application.bootstrap.shaderpipeline.ubomanager;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
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
import engine.root.ManagerPackage;
import engine.util.RegistryUtility;

public class UBOManager extends ManagerPackage {

    /*
     * Single authority on UBO lifetime, binding point allocation, and all GPU
     * operations. Owns all UBOHandles permanently. External callers receive a
     * UBOInstance via createUBOInstance() and push updates via push(). Handle
     * and Instance never touch GL directly.
     */

    // Internal
    private int nextAvailableBinding;
    private IntOpenHashSet usedBindings;
    private IntOpenHashSet releasedBindings;

    // Palette
    private Object2IntOpenHashMap<String> uboName2UBOID;
    private Int2ObjectOpenHashMap<UBOHandle> uboID2UBOHandle;

    // Base \\

    @Override
    protected void create() {

        this.nextAvailableBinding = 0;
        this.usedBindings = new IntOpenHashSet();
        this.releasedBindings = new IntOpenHashSet();
        this.uboName2UBOID = new Object2IntOpenHashMap<>();
        this.uboID2UBOHandle = new Int2ObjectOpenHashMap<>();

        create(InternalLoader.class);
    }

    @Override
    protected void dispose() {

        for (UBOHandle handle : uboID2UBOHandle.values())
            GLSLUtility.deleteUniformBuffer(handle.getGpuHandle());

        uboName2UBOID.clear();
        uboID2UBOHandle.clear();
        usedBindings.clear();
        releasedBindings.clear();
    }

    // Management \\

    void buildBuffer(UBOHandle handle) {

        String blockName = handle.getBlockName();

        if (uboName2UBOID.containsKey(blockName))
            return;

        int binding = resolveBinding(handle.getRequestedBinding(), blockName);
        int id = RegistryUtility.toIntID(blockName);
        int gpuHandle = GLSLUtility.createUniformBuffer();
        int totalSize = computeStd140BufferSize(handle.getUniformDeclarations());

        handle.initRuntime(id, gpuHandle, binding, totalSize);

        populateUniforms(handle);

        GLSLUtility.allocateUniformBuffer(gpuHandle, totalSize);
        GLSLUtility.bindUniformBufferBase(gpuHandle, binding);

        uboName2UBOID.put(blockName, id);
        uboID2UBOHandle.put(id, handle);
    }

    public UBOInstance createUBOInstance(UBOHandle handle) {

        if (handle == null)
            throwException("Cannot create UBO instance — handle is null");

        int newGpuHandle = GLSLUtility.createUniformBuffer();
        GLSLUtility.allocateUniformBuffer(newGpuHandle, handle.getTotalSizeBytes());
        GLSLUtility.bindUniformBufferBase(newGpuHandle, handle.getBindingPoint());

        UBOData clonedData = new UBOData(handle.getUBOData(), newGpuHandle);
        UBOInstance instance = create(UBOInstance.class);
        instance.constructor(clonedData);

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
        GLSLUtility.deleteUniformBuffer(instance.getGpuHandle());
    }

    public void push(UBOHandle handle) {
        ByteBuffer staging = handle.getUBOData().getStagingBuffer();
        staging.rewind();
        GLSLUtility.updateUniformBuffer(handle.getGpuHandle(), 0, staging);
    }

    public void push(UBOInstance instance) {
        ByteBuffer staging = instance.getUBOData().getStagingBuffer();
        staging.rewind();
        GLSLUtility.updateUniformBuffer(instance.getGpuHandle(), 0, staging);
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
        ((InternalLoader) internalLoader).request(blockName);
    }

    public boolean hasUBO(String uboName) {
        return uboName2UBOID.containsKey(uboName);
    }

    public int getUBOIDFromUBOName(String uboName) {

        if (!uboName2UBOID.containsKey(uboName))
            request(uboName);

        return uboName2UBOID.getInt(uboName);
    }

    public UBOHandle getUBOHandleFromUBOID(int uboID) {

        UBOHandle handle = uboID2UBOHandle.get(uboID);

        if (handle == null)
            throwException("UBO ID not found: " + uboID);

        return handle;
    }

    public UBOHandle getUBOHandleFromUBOName(String uboName) {
        return getUBOHandleFromUBOID(getUBOIDFromUBOName(uboName));
    }

    public UBOHandle findUBOHandle(String blockName) {

        if (uboName2UBOID.containsKey(blockName))
            return uboID2UBOHandle.get(uboName2UBOID.getInt(blockName));

        try {
            request(blockName);
        } catch (Exception e) {
            return null;
        }

        if (!uboName2UBOID.containsKey(blockName))
            return null;

        return uboID2UBOHandle.get(uboName2UBOID.getInt(blockName));
    }
}