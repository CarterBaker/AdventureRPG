package com.AdventureRPG.core.shaders.uniforms;

import com.AdventureRPG.core.engine.DataFrame;

public class UniformData extends DataFrame {

    // Internal
    private final UniformType uniformType;
    private final String uniformName;
    private final int count;

    public UniformData(
            UniformType uniformType,
            String uniformName) {

        // Internal
        this.uniformType = uniformType;
        this.uniformName = uniformName;
        this.count = 1;
    }

    public UniformData(
            UniformType uniformType,
            String uniformName,
            int count) {

        // Internal
        this.uniformType = uniformType;
        this.uniformName = uniformName;
        this.count = count;
    }

    // Data \\

    public UniformType uniformType() {
        return uniformType;
    }

    public String uniformName() {
        return uniformName;
    }

    public int count() {
        return count;
    }
}
