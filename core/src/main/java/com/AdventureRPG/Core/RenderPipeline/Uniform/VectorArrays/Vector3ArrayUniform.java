package com.AdventureRPG.core.renderpipeline.uniform.vectorarrays;

import com.AdventureRPG.core.renderpipeline.uniform.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Vectors.Vector3;

public class Vector3ArrayUniform extends UniformAttribute<Vector3[]> {

    // Base \\

    public Vector3ArrayUniform(int count) {

        super(new Vector3[count]);

        for (int i = 0; i < count; i++)
            value[i] = new Vector3();
    }

    // Utility \\

    @Override
    protected void push(int handle, Vector3[] value) {

    }
}
