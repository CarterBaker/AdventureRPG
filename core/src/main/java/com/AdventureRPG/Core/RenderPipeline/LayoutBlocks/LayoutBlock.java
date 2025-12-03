package com.AdventureRPG.Core.RenderPipeline.LayoutBlocks;

import com.AdventureRPG.Core.RenderPipeline.Uniforms.Uniform;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class LayoutBlock {

    // Internal
    public final String layoutName;
    public final int layoutID;
    public final int layoutHandle;

    private final Object2ObjectOpenHashMap<String, Uniform<?>> uniforms;

    public LayoutBlock(
            String layoutName,
            int layoutID,
            int layoutHandle) {

        // Internal
        this.layoutName = layoutName;
        this.layoutID = layoutID;
        this.layoutHandle = layoutHandle;

        this.uniforms = new Object2ObjectOpenHashMap<>();
    }

    // Utility \\

    public void addUniform(String uniformName, Uniform<?> uniform) {
        uniforms.put(uniformName, uniform);
    }

    public Uniform<?> getUniform(String uniformName) {
        return uniforms.get(uniformName);
    }

    public Object2ObjectOpenHashMap<String, Uniform<?>> getUniforms() {
        return uniforms;
    }
}
