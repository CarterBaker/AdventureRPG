package com.AdventureRPG.Core.RenderPipeline.Materials;

import com.AdventureRPG.Core.RenderPipeline.Uniforms.Uniform;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class Material {

    // Internal
    public final String materialName;
    public final int materialID;
    public final int shaderID;

    private Object2ObjectOpenHashMap<String, Uniform<?>> uniforms;

    public Material(
            String materialName,
            int materialID,
            int shaderID,
            Object2ObjectOpenHashMap<String, Uniform<?>> uniforms) {

        // Internal
        this.materialName = materialName;
        this.materialID = materialID;
        this.shaderID = shaderID;

        this.uniforms = uniforms;
    }

    // Accessible \\

    public Uniform<?> getUniform(String uniformName) {
        return uniforms.get(uniformName);
    }
}
