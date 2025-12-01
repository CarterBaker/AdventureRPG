package com.AdventureRPG.Core.RenderPipeline.Uniforms.Scalars;

import com.AdventureRPG.Core.RenderPipeline.Uniforms.UniformAttribute;

public class IntegerUniform extends UniformAttribute<Integer> {

    // Base \\

    public IntegerUniform() {
        super(0);
    }

    // Utility \\

    @Override
    protected void push(int handle, Integer value) {

    }
}
