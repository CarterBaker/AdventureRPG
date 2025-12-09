package com.AdventureRPG.core.shaderpipeline.uniform.scalars;

import com.AdventureRPG.core.shaderpipeline.uniform.UniformAttribute;

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
