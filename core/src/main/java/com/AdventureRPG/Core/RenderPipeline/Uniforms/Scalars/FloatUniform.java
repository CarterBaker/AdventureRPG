package com.AdventureRPG.Core.RenderPipeline.Uniforms.Scalars;

import com.AdventureRPG.Core.RenderPipeline.Uniforms.UniformAttribute;

public class FloatUniform extends UniformAttribute<Float> {

    // Base \\

    public FloatUniform() {
        super(0f);
    }

    // Utility \\

    @Override
    protected void push(int handle, Float value) {

    }
}