package com.AdventureRPG.Core.RenderPipeline.Uniforms.Samplers;

import com.AdventureRPG.Core.RenderPipeline.Uniforms.UniformAttribute;

public class SampleImage2DArrayUniform extends UniformAttribute<Integer> {

    // Base \\

    public SampleImage2DArrayUniform() {
        super(0);
    }

    public SampleImage2DArrayUniform(int value) {
        super(value);
    }

    // Utility \\

    @Override
    protected void push(int handle, Integer value) {

    }
}
