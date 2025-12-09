package com.AdventureRPG.core.shaderpipeline.uniform.matrixArrays;

import com.AdventureRPG.core.shaderpipeline.uniform.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Matrices.Matrix4Double;

public class Matrix4DoubleArrayUniform extends UniformAttribute<Matrix4Double[]> {

    // Base \\

    public Matrix4DoubleArrayUniform(int count) {

        super(new Matrix4Double[count]);

        for (int i = 0; i < count; i++)
            value[i] = new Matrix4Double();
    }

    // Utility \\

    @Override
    protected void push(int handle, Matrix4Double[] value) {

    }
}
