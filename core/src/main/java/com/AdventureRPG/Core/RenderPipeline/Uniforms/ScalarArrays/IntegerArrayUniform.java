package com.AdventureRPG.Core.RenderPipeline.Uniforms.ScalarArrays;

import com.AdventureRPG.Core.RenderPipeline.Uniforms.UniformAttribute;

public class IntegerArrayUniform extends UniformAttribute<Integer[]> {

    // Base \\

    public IntegerArrayUniform(int count) {

        super(new Integer[count]);

        for (int i = 0; i < count; i++)
            value[i] = 0;
    }

    // Utility \\

    @Override
    protected void push(int handle, Integer[] value) {

    }
}
