package com.AdventureRPG.core.shaderpipeline.uniform.vectorarrays;

import com.AdventureRPG.core.shaderpipeline.uniform.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Vectors.Vector4;

public class Vector4ArrayUniform extends UniformAttribute<Vector4[]> {

    // Base \\

    public Vector4ArrayUniform(int count) {

        super(new Vector4[count]);

        for (int i = 0; i < count; i++)
            value[i] = new Vector4();
    }

    // Utility \\

    @Override
    protected void push(int handle, Vector4[] value) {

    }
}
