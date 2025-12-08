package com.AdventureRPG.core.renderpipeline.uniform.scalarArrays;

import com.AdventureRPG.core.renderpipeline.uniform.UniformAttribute;

public class FloatArrayUniform extends UniformAttribute<Float[]> {

    // Base \\

    public FloatArrayUniform(int count) {

        super(new Float[count]);

        for (int i = 0; i < count; i++)
            value[i] = 0.0f;
    }

    // Utility \\

    @Override
    protected void push(int handle, Float[] value) {

    }
}