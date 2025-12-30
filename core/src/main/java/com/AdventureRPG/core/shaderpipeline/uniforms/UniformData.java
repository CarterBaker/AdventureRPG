package com.AdventureRPG.core.shaderpipeline.uniforms;

import com.AdventureRPG.core.engine.DataPackage;

public class UniformData extends DataPackage {

    // Internal
    private final UniformType uniformType;
    private final int count;

    public UniformData(
            UniformType uniformType,
            String name) {

        // Internal
        super(
                name,
                0);

        this.uniformType = uniformType;
        this.count = 1;
    }

    public UniformData(
            UniformType uniformType,
            String name,
            int count) {

        // Internal
        super(
                name,
                0);

        this.uniformType = uniformType;
        this.count = count;
    }

    // Data \\

    public UniformType uniformType() {
        return uniformType;
    }

    public int count() {
        return count;
    }
}
