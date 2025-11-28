package com.AdventureRPG.Core.RenderPipeline.Uniforms.Scalars;

import com.AdventureRPG.Core.RenderPipeline.Uniforms.UniformAttribute;

public class DoubleUniform extends UniformAttribute<Double> {

    // Base \\

    public DoubleUniform() {
        super(0.0);
    }

    public DoubleUniform(Double value) {
        super(value);
    }

    // Utility \\

    @Override
    protected void push(int handle, Double value) {

    }
}
