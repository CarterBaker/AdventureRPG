package com.AdventureRPG.core.renderpipeline.uniform.scalarArrays;

import com.AdventureRPG.core.renderpipeline.uniform.UniformAttribute;

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
