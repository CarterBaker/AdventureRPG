package com.AdventureRPG.bootstrap.shaderpipeline.ubomanager;

import java.nio.ByteBuffer;

import com.AdventureRPG.bootstrap.shaderpipeline.uniforms.Uniform;
import com.AdventureRPG.core.engine.HandlePackage;
import com.badlogic.gdx.utils.BufferUtils;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public final class UBOHandle extends HandlePackage {

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

        // Internal
        this.bufferName = bufferName;
        this.bufferID = bufferID;
        this.gpuHandle = gpuHandle;
        this.bindingPoint = bindingPoint;
        this.totalSizeBytes = totalSizeBytes;

        this.stagingBuffer = BufferUtils.newByteBuffer(totalSizeBytes);
        this.uniforms = new Object2ObjectOpenHashMap<>();
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

    public void addUniform(String uniformName, Uniform<?> uniform) {
        uniforms.put(uniformName, uniform);
    }

    public Uniform<?> getUniform(String uniformName) {
        return uniforms.get(uniformName);
    }

    public Object2ObjectOpenHashMap<String, Uniform<?>> getUniforms() {
        return uniforms;
    }

    public void updateUniform(String uniformName, Object value) {

        Uniform<?> uniform = uniforms.get(uniformName);

        if (uniform == null)
            throwException(
                    "Uniform not found in UBO '" + bufferName + "': " + uniformName);

        // Set the value in the uniform's attribute
        @SuppressWarnings("unchecked")
        Uniform<Object> typedUniform = (Uniform<Object>) uniform;
        typedUniform.attribute().set(value);

        // Copy to staging buffer at correct offset
        ByteBuffer uniformData = uniform.attribute().getByteBuffer();
        uniformData.rewind(); // ensure position is at start

        stagingBuffer.position(uniform.offset);
        stagingBuffer.put(uniformData);
    }

    public void push() {
        stagingBuffer.rewind(); // reset to start
        GLSLUtility.updateUniformBuffer(gpuHandle, 0, stagingBuffer);
    }
}