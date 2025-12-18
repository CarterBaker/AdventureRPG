package com.AdventureRPG.core.shaders.materials;

import com.AdventureRPG.core.shaders.shaders.Shader;
import com.AdventureRPG.core.shaders.ubomanager.UBOHandle;
import com.AdventureRPG.core.shaders.uniforms.Uniform;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class Material {

    // Internal
    public final String materialName;
    public final int materialID;

    public final Shader shader;
    private final Object2ObjectOpenHashMap<String, UBOHandle> buffers;
    private Object2ObjectOpenHashMap<String, Uniform<?>> uniforms;

    public Material(
            String materialName,
            int materialID,
            Shader shader,
            Object2ObjectOpenHashMap<String, UBOHandle> buffers,
            Object2ObjectOpenHashMap<String, Uniform<?>> uniforms) {

        // Internal
        this.materialName = materialName;
        this.materialID = materialID;

        this.shader = shader;
        this.buffers = buffers;
        this.uniforms = uniforms;
    }

    // Accessible \\

    public UBOHandle getUBO(String ubo) {
        return buffers.get(ubo);
    }

    public Uniform<?> getUniform(String uniformName) {
        return uniforms.get(uniformName);
    }
}
