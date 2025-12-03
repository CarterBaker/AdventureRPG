package com.AdventureRPG.Core.RenderPipeline.Uniform.Scalars;

import com.AdventureRPG.Core.RenderPipeline.Uniform.UniformAttribute;

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
