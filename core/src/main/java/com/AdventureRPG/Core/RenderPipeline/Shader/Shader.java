package com.AdventureRPG.Core.RenderPipeline.Shader;

import com.AdventureRPG.Core.RenderPipeline.LayoutBlocks.LayoutBlock;
import com.AdventureRPG.Core.RenderPipeline.Uniform.Uniform;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class Shader {

    // Internal
    public final String shaderName;
    public final int shaderID;
    public final int shaderHandle;

    private final Object2ObjectOpenHashMap<String, LayoutBlock> layouts;
    private final Object2ObjectOpenHashMap<String, Uniform<?>> uniforms;

    public Shader(
            String shaderName,
            int shaderID,
            int shaderHandle) {

        // Internal
        this.shaderName = shaderName;
        this.shaderID = shaderID;
        this.shaderHandle = shaderHandle;

        this.layouts = new Object2ObjectOpenHashMap<>();
        this.uniforms = new Object2ObjectOpenHashMap<>();
    }

    // Utility \\

    // Layouts
    public void addLayout(String layoutName, LayoutBlock layout) {
        layouts.put(layoutName, layout);
    }

    // Uniforms
    public void addUniform(String uniformName, Uniform<?> uniform) {
        uniforms.put(uniformName, uniform);
    }

    public Object2ObjectOpenHashMap<String, Uniform<?>> getUniforms() {
        return uniforms;
    }
}
