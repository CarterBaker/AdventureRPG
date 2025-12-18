package com.AdventureRPG.core.shaders.ubomanager;

import com.AdventureRPG.core.engine.HandleFrame;
import com.AdventureRPG.core.shaders.uniforms.Uniform;
import com.AdventureRPG.core.util.Exceptions.GraphicException;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public final class UBOHandle extends HandleFrame {

    // Identification
    public final String bufferName;
    public final int bufferID;
    public final int gpuHandle;
    public final int bindingPoint;

    // Uniform storage
    private final Object2ObjectOpenHashMap<String, Uniform<?>> uniforms;

    public UBOHandle(
            String bufferName,
            int bufferID,
            int gpuHandle,
            int bindingPoint) {

        this.bufferName = bufferName;
        this.bufferID = bufferID;
        this.gpuHandle = gpuHandle;
        this.bindingPoint = bindingPoint;

        this.uniforms = new Object2ObjectOpenHashMap<>();
    }

    // Build-time: add uniforms with offsets
    public void addUniform(String uniformName, Uniform<?> uniform) {
        uniforms.put(uniformName, uniform);
    }

    // Runtime: update a specific uniform
    public void update(String uniformName, Object value) {

        Uniform<?> uniform = uniforms.get(uniformName);

        if (uniform == null)
            throw new GraphicException.ShaderProgramException(
                    "Uniform not found in UBO '" + bufferName + "': " + uniformName);

        // Set the value in the uniform's attribute
        @SuppressWarnings("unchecked")
        Uniform<Object> typedUniform = (Uniform<Object>) uniform;
        typedUniform.attribute().set(value);

        // Upload to GPU at the correct offset
        GLSLUtility.updateUniformBuffer(
                gpuHandle,
                uniform.offset(),
                uniform.attribute().getByteBuffer());
    }

    // Utility
    public Uniform<?> getUniform(String uniformName) {
        return uniforms.get(uniformName);
    }

    public Object2ObjectOpenHashMap<String, Uniform<?>> getUniforms() {
        return uniforms;
    }
}