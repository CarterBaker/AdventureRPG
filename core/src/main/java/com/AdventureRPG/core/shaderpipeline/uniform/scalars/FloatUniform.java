package com.AdventureRPG.core.shaderpipeline.uniform.scalars;

import com.AdventureRPG.core.shaderpipeline.uniform.UniformAttribute;

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