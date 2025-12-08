package com.AdventureRPG.core.renderpipeline.uniform.scalarArrays;

import com.AdventureRPG.core.renderpipeline.uniform.UniformAttribute;

public class BooleanArrayUniform extends UniformAttribute<Boolean[]> {

    // Base \\

    public BooleanArrayUniform(int count) {

        super(new Boolean[count]);

        for (int i = 0; i < count; i++)
            value[i] = Boolean.FALSE;
    }

    // Utility \\

    @Override
    protected void push(int handle, Boolean[] value) {

    }
}
