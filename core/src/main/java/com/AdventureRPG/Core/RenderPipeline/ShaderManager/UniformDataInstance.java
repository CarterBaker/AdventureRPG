package com.AdventureRPG.Core.RenderPipeline.ShaderManager;

import com.AdventureRPG.Core.Bootstrap.InstanceFrame;

public class UniformDataInstance extends InstanceFrame {

    // Internal
    public final UniformType uniformType;
    public final String uniformName;

    public UniformDataInstance(
            UniformType uniformType,
            String uniformName) {

        // Internal
        this.uniformType = uniformType;
        this.uniformName = uniformName;
    }
}
