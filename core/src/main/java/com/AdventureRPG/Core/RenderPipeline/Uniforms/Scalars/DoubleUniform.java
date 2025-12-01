package com.AdventureRPG.Core.RenderPipeline.Uniforms.Scalars;

import com.AdventureRPG.Core.RenderPipeline.Uniforms.UniformAttribute;

public class DoubleUniform extends UniformAttribute<Double> {

    // Base \\

    public DoubleUniform() {
        super(0.0);
    }

    // Utility \\

    @Override
    protected void push(int handle, Double value) {

    }
}
