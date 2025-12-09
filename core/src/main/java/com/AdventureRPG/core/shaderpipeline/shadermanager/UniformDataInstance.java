package com.AdventureRPG.core.shaderpipeline.shadermanager;

import com.AdventureRPG.core.kernel.InstanceFrame;

public class UniformDataInstance extends InstanceFrame {

    // Internal
    private final UniformType uniformType;
    private final String uniformName;
    private final int count;

    public UniformDataInstance(
            UniformType uniformType,
            String uniformName) {

        // Internal
        this.uniformType = uniformType;
        this.uniformName = uniformName;
        this.count = 1;
    }

    public UniformDataInstance(
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
