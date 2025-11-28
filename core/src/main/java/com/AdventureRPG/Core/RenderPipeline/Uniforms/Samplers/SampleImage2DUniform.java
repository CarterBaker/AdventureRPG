package com.AdventureRPG.Core.RenderPipeline.Uniforms.Samplers;

import com.AdventureRPG.Core.RenderPipeline.Uniforms.UniformAttribute;

public class SampleImage2DUniform extends UniformAttribute<Integer> {

    // Base \\

    public SampleImage2DUniform() {
        super(0);
    }

    public SampleImage2DUniform(int value) {
        super(value);
    }

    // Utility \\

    @Override
    protected void push(int handle, Integer value) {

    }
}
