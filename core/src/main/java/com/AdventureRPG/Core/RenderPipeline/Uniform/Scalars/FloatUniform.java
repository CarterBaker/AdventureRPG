package com.AdventureRPG.core.renderpipeline.uniform.scalars;

import com.AdventureRPG.core.renderpipeline.uniform.UniformAttribute;

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