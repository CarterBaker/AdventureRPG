package com.AdventureRPG.core.shaderpipeline.ubomanager;

import com.AdventureRPG.core.engine.DataPackage;
import com.AdventureRPG.core.shaderpipeline.uniforms.UniformData;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class UBOData extends DataPackage {

    // Internal
    private final ObjectArrayList<UniformData> uniforms;

    public UBOData(
            String name,
            int id) {

        // Internal
        super(
                name,
                id);

        this.uniforms = new ObjectArrayList<>();
    }

    // Accessible \\

    // Uniforms
    public void addUniform(UniformData uniform) {
        uniforms.add(uniform);
    }

    public ObjectArrayList<UniformData> getUniforms() {
        return uniforms;
    }
}
