package com.AdventureRPG.Core.RenderPipeline.Uniforms.Scalars;

import com.AdventureRPG.Core.RenderPipeline.Uniforms.UniformAttribute;

public class BooleanUniform extends UniformAttribute<Boolean> {

    // Base \\

    public BooleanUniform() {
        super(false);
    }

    // Utility \\

    @Override
    protected void push(int handle, Boolean value) {

    }
}
