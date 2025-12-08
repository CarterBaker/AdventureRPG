package com.AdventureRPG.core.renderpipeline.uniform.vectorarrays;

import com.AdventureRPG.core.renderpipeline.uniform.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Vectors.Vector2Double;

public class Vector2DoubleArrayUniform extends UniformAttribute<Vector2Double[]> {

    // Base \\

    public Vector2DoubleArrayUniform(int count) {

        super(new Vector2Double[count]);

        for (int i = 0; i < count; i++)
            value[i] = new Vector2Double();
    }

    // Utility \\

    @Override
    protected void push(int handle, Vector2Double[] value) {

    }
}
