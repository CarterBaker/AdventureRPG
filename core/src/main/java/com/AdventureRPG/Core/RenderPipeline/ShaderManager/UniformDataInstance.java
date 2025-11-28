package com.AdventureRPG.Core.RenderPipeline.ShaderManager;

import com.AdventureRPG.Core.Bootstrap.InstanceFrame;

public class UniformDataInstance extends InstanceFrame {

    // Internal
    public final UniformType uniformType;
    public final String uniformName;
    public final String uniformData;

    public UniformDataInstance(
            UniformType uniformType,
            String uniformName,
            String uniformData) {

        // Internal
        this.uniformType = uniformType;
        this.uniformName = uniformName;
        this.uniformData = uniformData;
    }
}
