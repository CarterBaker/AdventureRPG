package com.AdventureRPG.core.renderpipeline.uniform.scalars;

import com.AdventureRPG.core.renderpipeline.uniform.UniformAttribute;

public class BooleanUniform extends UniformAttribute<Boolean> {

    // Base \\

    public BooleanUniform() {
        super(false);
    }

    // Utility \\

    @Override
    protected void push(int handle, Boolean value) {

    }
}
