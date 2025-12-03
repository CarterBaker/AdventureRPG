package com.AdventureRPG.Core.RenderPipeline.Uniform.Scalars;

import com.AdventureRPG.Core.RenderPipeline.Uniform.UniformAttribute;

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
