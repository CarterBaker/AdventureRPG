package com.AdventureRPG.core.shaderpipeline.uniform.scalars;

import com.AdventureRPG.core.shaderpipeline.uniform.UniformAttribute;

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
