package com.internal.bootstrap.shaderpipeline.ubo;

import java.nio.ByteBuffer;

import com.badlogic.gdx.utils.BufferUtils;
import com.internal.bootstrap.shaderpipeline.ubomanager.GLSLUtility;
import com.internal.bootstrap.shaderpipeline.uniforms.Uniform;
import com.internal.core.engine.InstancePackage;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

/*
 * Runtime UBO handed to external systems by UBOManager.cloneUBO().
 * Owns its own GPU buffer and staging buffer. Shares the source handle's
 * binding point so the shader block binding remains valid.
 * Must be released via UBOManager.destroyInstance() — never dispose directly.
 */
public class UBOInstance extends InstancePackage {

    // Internal
    private String bufferName;
    private int gpuHandle;
    private int bindingPoint;
    private int totalSizeBytes;
    private ByteBuffer stagingBuffer;
    private Object2ObjectOpenHashMap<String, Uniform<?>> uniforms;

    // Internal \\

    public void constructor(
            String bufferName,
            int gpuHandle,
            int bindingPoint,
            int totalSizeBytes) {
        this.bufferName = bufferName;
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

    public int getGpuHandle() {
        return gpuHandle;
    }

    public int getBindingPoint() {
        return bindingPoint;
    }

    public int getTotalSizeBytes() {
        return totalSizeBytes;
    }
}