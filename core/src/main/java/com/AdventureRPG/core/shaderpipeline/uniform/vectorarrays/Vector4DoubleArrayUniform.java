package com.AdventureRPG.core.shaderpipeline.uniform.vectorarrays;

import com.AdventureRPG.core.shaderpipeline.uniform.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Vectors.Vector4Double;

public class Vector4DoubleArrayUniform extends UniformAttribute<Vector4Double[]> {

    // Base \\

    public Vector4DoubleArrayUniform(int count) {

        super(new Vector4Double[count]);

        for (int i = 0; i < count; i++)
            value[i] = new Vector4Double();
    }

    // Utility \\

    @Override
    protected void push(int handle, Vector4Double[] value) {

    }
}
