package com.internal.bootstrap.shaderpipeline.ubo;

import java.nio.ByteBuffer;

import com.badlogic.gdx.utils.BufferUtils;
import com.internal.bootstrap.shaderpipeline.ubomanager.GLSLUtility;
import com.internal.bootstrap.shaderpipeline.uniforms.Uniform;
import com.internal.core.engine.HandlePackage;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

/*
 * Persistent GPU-backed uniform buffer block owned exclusively by UBOManager.
 * Never handed to external systems — external callers receive a UBOInstance via
 * UBOManager.cloneUBO(). Holds the canonical std140 layout and serves as the
 * template from which instances are built.
 */
public class UBOHandle extends HandlePackage {

    // Internal
    private String bufferName;
    private int bufferID;
    private int gpuHandle;
    private int bindingPoint;
    private int totalSizeBytes;
    private ByteBuffer stagingBuffer;
    private Object2ObjectOpenHashMap<String, Uniform<?>> uniforms;

    // Internal \\

    public void constructor(
            String bufferName,
            int bufferID,
            int gpuHandle,
            int bindingPoint,
            int totalSizeBytes) {
        this.bufferName = bufferName;
        this.bufferID = bufferID;
        this.gpuHandle = gpuHandle;
        this.bindingPoint = bindingPoint;
        this.totalSizeBytes = totalSizeBytes;
        this.stagingBuffer = BufferUtils.newByteBuffer(totalSizeBytes);
        this.uniforms = new Object2ObjectOpenHashMap<>();
    }

    // Utility \\

    public void addUniform(String name, Uniform<?> uniform) {
        uniforms.put(name, uniform);
    }

    public void updateUniform(String name, Object value) {

        Uniform<?> uniform = uniforms.get(name);

        if (uniform == null)
            throwException("Uniform not found in UBO '" + bufferName + "': " + name);

        uniform.attribute().setObject(value);

        ByteBuffer data = uniform.attribute().getByteBuffer();
        data.rewind();
        stagingBuffer.position(uniform.offset);
        stagingBuffer.put(data);
    }

    public void push() {
        stagingBuffer.rewind();
        GLSLUtility.updateUniformBuffer(gpuHandle, 0, stagingBuffer);
    }

    // Accessible \\

    public String getBufferName() {
        return bufferName;
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

    public Uniform<?> getUniform(String name) {
        return uniforms.get(name);
    }

    public Object2ObjectOpenHashMap<String, Uniform<?>> getUniforms() {
        return uniforms;
    }
}