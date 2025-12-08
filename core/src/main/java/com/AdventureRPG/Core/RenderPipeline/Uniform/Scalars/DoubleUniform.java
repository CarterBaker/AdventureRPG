package com.AdventureRPG.core.renderpipeline.uniform.scalars;

import com.AdventureRPG.core.renderpipeline.uniform.UniformAttribute;

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
