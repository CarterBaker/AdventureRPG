package com.AdventureRPG.Core.RenderPipeline.ShaderManager;

import com.AdventureRPG.Core.Bootstrap.InstanceFrame;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class LayoutDataInstance extends InstanceFrame {

    private final String blockName;
    private final int binding;
    private final Int2ObjectOpenHashMap<UniformDataInstance> uniforms;

    public LayoutDataInstance(
            String blockName,
            int binding) {

        this.blockName = blockName;
        this.binding = binding;
        this.uniforms = new Int2ObjectOpenHashMap<>();
    }

    public void addUniform(int index, UniformDataInstance u) {
        uniforms.put(index, u);
    }

    public String getBlockName() {
        return blockName;
    }

    public int getBinding() {
        return binding;
    }

    public Int2ObjectOpenHashMap<UniformDataInstance> getUniforms() {
        return uniforms;
    }
}
