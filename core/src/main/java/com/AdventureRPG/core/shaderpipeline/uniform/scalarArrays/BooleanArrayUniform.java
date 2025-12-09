package com.AdventureRPG.core.shaderpipeline.uniform.scalarArrays;

import com.AdventureRPG.core.shaderpipeline.uniform.UniformAttribute;

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
