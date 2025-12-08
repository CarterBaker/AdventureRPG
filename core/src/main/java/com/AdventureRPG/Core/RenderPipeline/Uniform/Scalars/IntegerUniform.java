package com.AdventureRPG.core.renderpipeline.uniform.scalars;

import com.AdventureRPG.core.renderpipeline.uniform.UniformAttribute;

public class IntegerUniform extends UniformAttribute<Integer> {

    // Base \\

    public IntegerUniform() {
        super(0);
    }

    // Utility \\

    @Override
    protected void push(int handle, Integer value) {

    }
}
