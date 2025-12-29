package com.AdventureRPG.core.shaderpipeline.ubomanager;

import com.AdventureRPG.core.engine.DataFrame;
import com.AdventureRPG.core.shaderpipeline.uniforms.UniformData;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class UBOData extends DataFrame {

    private final String blockName;
    private final int binding;
    private final ObjectArrayList<UniformData> uniforms;

    public UBOData(
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
    public void addUniform(UniformData uniform) {
        uniforms.add(uniform);
    }

    public ObjectArrayList<UniformData> getUniforms() {
        return uniforms;
    }
}
