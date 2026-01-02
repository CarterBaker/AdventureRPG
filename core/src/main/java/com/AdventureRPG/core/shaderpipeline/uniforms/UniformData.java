package com.AdventureRPG.core.shaderpipeline.uniforms;

import com.AdventureRPG.core.engine.DataPackage;

public class UniformData extends DataPackage {

    // Internal
    private UniformType uniformType;
    private String uniformName;
    private int count;

    // Internal

    public void awake(
            UniformType uniformType,
            String uniformName) {

        // Internal
        this.uniformType = uniformType;
        this.uniformName = uniformName;
        this.count = 1;
    }

    public void awake(
            UniformType uniformType,
            String uniformName,
            int count) {

        // Internal
        this.uniformType = uniformType;
        this.uniformName = uniformName;
        this.count = count;
    }

    // Data \\

    public UniformType getUniformType() {
        return uniformType;
    }

    public String getUniformName() {
        return uniformName;
    }

    public int getCount() {
        return count;
    }
}