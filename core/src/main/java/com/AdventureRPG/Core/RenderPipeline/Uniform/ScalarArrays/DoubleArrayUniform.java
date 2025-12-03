package com.AdventureRPG.Core.RenderPipeline.Uniform.ScalarArrays;

import com.AdventureRPG.Core.RenderPipeline.Uniform.UniformAttribute;

public class DoubleArrayUniform extends UniformAttribute<Double[]> {

    // Base \\

    public DoubleArrayUniform(int count) {

        super(new Double[count]);

        for (int i = 0; i < count; i++)
            value[i] = 0.0d;
    }

    // Utility \\

    @Override
    protected void push(int handle, Double[] value) {

    }
}
