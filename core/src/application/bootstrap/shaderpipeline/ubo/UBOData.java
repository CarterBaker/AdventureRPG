package application.bootstrap.shaderpipeline.ubo;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.nio.ByteBuffer;

import application.bootstrap.shaderpipeline.uniforms.UniformData;
import application.bootstrap.shaderpipeline.uniforms.UniformStruct;
import engine.root.DataPackage;
import engine.settings.EngineSetting;
import engine.util.memory.BufferUtils;

public class UBOData extends DataPackage {

    /*
     * Complete UBO record. Source-phase fields are set by the builder. Runtime
     * fields are set by UBOManager.buildBuffer() after GPU allocation. Owned by
     * UBOHandle or UBOInstance for the full session — nothing is discarded.
     * uniformKeys mirrors compiledUniforms for zero-allocation iteration.
     */

    public static final int UNSPECIFIED_BINDING = EngineSetting.SHADER_UBO_UNSPECIFIED_BINDING;

    // Source
    private final String blockName;
    private final int requestedBinding;
    private final ObjectArrayList<UniformData> uniformDeclarations;

    // Runtime
    private int bufferID;
    private int gpuHandle;
    private int bindingPoint;
    private int totalSizeBytes;
    private ByteBuffer stagingBuffer;
    private final Object2ObjectOpenHashMap<String, UniformStruct<?>> compiledUniforms;
    private final ObjectArrayList<String> uniformKeys;

    // Constructor — source phase \\

    public UBOData(String blockName, int requestedBinding) {

        this.blockName = blockName;
        this.requestedBinding = requestedBinding;
        this.uniformDeclarations = new ObjectArrayList<>();
        this.compiledUniforms = new Object2ObjectOpenHashMap<>();
        this.uniformKeys = new ObjectArrayList<>();
    }

    // Constructor — instance deep copy \\

    public UBOData(UBOData source, int newGpuHandle) {

        this.blockName = source.blockName;
        this.requestedBinding = source.requestedBinding;
        this.uniformDeclarations = source.uniformDeclarations;
        this.bufferID = EngineSetting.INDEX_NOT_FOUND;
        this.gpuHandle = newGpuHandle;
        this.bindingPoint = source.bindingPoint;
        this.totalSizeBytes = source.totalSizeBytes;
        this.stagingBuffer = BufferUtils.newByteBuffer(source.totalSizeBytes);
        this.compiledUniforms = new Object2ObjectOpenHashMap<>();
        this.uniformKeys = new ObjectArrayList<>(source.uniformKeys);
    }

    // Source Phase \\

    void addUniformDeclaration(UniformData uniform) {
        uniformDeclarations.add(uniform);
    }

    // Runtime Phase \\

    void initRuntime(int bufferID, int gpuHandle, int bindingPoint, int totalSizeBytes) {
        this.bufferID = bufferID;
        this.gpuHandle = gpuHandle;
        this.bindingPoint = bindingPoint;
        this.totalSizeBytes = totalSizeBytes;
        this.stagingBuffer = BufferUtils.newByteBuffer(totalSizeBytes);
    }

    void addCompiledUniform(String name, UniformStruct<?> uniform) {
        compiledUniforms.put(name, uniform);
        uniformKeys.add(name);
    }

    void updateUniform(String name, Object value) {

        UniformStruct<?> uniform = compiledUniforms.get(name);

        if (uniform == null)
            throwException("Uniform not found in UBO '" + blockName + "': " + name);

        uniform.attribute().setObject(value);

        ByteBuffer data = uniform.attribute().getByteBuffer();
        data.rewind();
        stagingBuffer.position(uniform.getOffset());
        stagingBuffer.put(data);
    }

    public ByteBuffer getStagingBuffer() {
        return stagingBuffer;
    }

    // Accessible \\

    public String getBlockName() {
        return blockName;
    }

    public int getRequestedBinding() {
        return requestedBinding;
    }

    public ObjectArrayList<UniformData> getUniformDeclarations() {
        return uniformDeclarations;
    }

    public int getBufferID() {
        return bufferID;
    }

    public int getGpuHandle() {
        return gpuHandle;
    }

    public int getBindingPoint() {
        return bindingPoint;
    }

    public int getTotalSizeBytes() {
        return totalSizeBytes;
    }

    public ObjectArrayList<String> getUniformKeys() {
        return uniformKeys;
    }

    public UniformStruct<?> getCompiledUniform(String name) {
        return compiledUniforms.get(name);
    }

    public Object2ObjectOpenHashMap<String, UniformStruct<?>> getCompiledUniforms() {
        return compiledUniforms;
    }
}