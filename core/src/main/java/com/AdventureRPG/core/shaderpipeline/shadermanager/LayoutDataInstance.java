package com.AdventureRPG.core.shaderpipeline.shadermanager;

import com.AdventureRPG.core.kernel.InstanceFrame;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class LayoutDataInstance extends InstanceFrame {

    private final String blockName;
    private final int binding;
    private final ObjectArrayList<UniformDataInstance> uniforms;

    public LayoutDataInstance(
            String blockName,
            int binding) {

        this.blockName = blockName;
        this.binding = binding;
        this.uniforms = new ObjectArrayList<>();
    }

    // Accessible \\

    // Data
    public String blockName() {
        return blockName;
    }

    public int binding() {
        return binding;
    }

    // Uniforms
    public void addUniform(UniformDataInstance uniform) {
        uniforms.add(uniform);
    }

    public ObjectArrayList<UniformDataInstance> getUniforms() {
        return uniforms;
    }
}
