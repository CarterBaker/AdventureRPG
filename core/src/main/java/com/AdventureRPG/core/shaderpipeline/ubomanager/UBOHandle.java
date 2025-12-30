package com.AdventureRPG.core.shaderpipeline.ubomanager;

import java.nio.ByteBuffer;

import com.AdventureRPG.core.engine.HandlePackage;
import com.AdventureRPG.core.shaderpipeline.uniforms.Uniform;
import com.badlogic.gdx.utils.BufferUtils;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public final class UBOHandle extends HandlePackage {

    // Internal
    public final String bufferName;
    public final int bufferID;
    public final int gpuHandle;
    public final int bindingPoint;
    public final int totalSizeBytes;

    private final ByteBuffer stagingBuffer;
    private final Object2ObjectOpenHashMap<String, Uniform<?>> uniforms;

    public UBOHandle(
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

    // Utility \\
    public void addUniform(String uniformName, Uniform<?> uniform) {
        uniforms.put(uniformName, uniform);
    }

    // Utility \\

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