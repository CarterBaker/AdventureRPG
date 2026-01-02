package com.AdventureRPG.core.shaderpipeline.ubomanager;

import com.AdventureRPG.core.engine.DataPackage;
import com.AdventureRPG.core.shaderpipeline.uniforms.UniformData;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class UBOData extends DataPackage {

    // Internal
    private String blockName;
    private int binding;
    private ObjectArrayList<UniformData> uniforms;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.uniforms = new ObjectArrayList<>();
    }

    public void awake(
            String blockName,
            int binding) {

        // Internal
        this.blockName = blockName;
        this.binding = binding;
    }

    // Accessible \\

    // Internal
    public String getBlockName() {
        return blockName;
    }

    public int getBinding() {
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