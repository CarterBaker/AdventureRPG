package com.AdventureRPG.core.shaderpipeline.uniform.vectorarrays;

import com.AdventureRPG.core.shaderpipeline.uniform.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Vectors.Vector3Double;

public class Vector3DoubleArrayUniform extends UniformAttribute<Vector3Double[]> {

    // Base \\

    public Vector3DoubleArrayUniform(int count) {

        super(new Vector3Double[count]);

        for (int i = 0; i < count; i++)
            value[i] = new Vector3Double();
    }

    // Utility \\

    @Override
    protected void push(int handle, Vector3Double[] value) {

    }
}
