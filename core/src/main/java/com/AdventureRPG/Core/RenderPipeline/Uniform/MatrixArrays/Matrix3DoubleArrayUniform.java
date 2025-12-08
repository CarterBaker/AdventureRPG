package com.AdventureRPG.core.renderpipeline.uniform.matrixArrays;

import com.AdventureRPG.core.renderpipeline.uniform.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Matrices.Matrix3Double;

public class Matrix3DoubleArrayUniform extends UniformAttribute<Matrix3Double[]> {

    // Base \\

    public Matrix3DoubleArrayUniform(int count) {

        super(new Matrix3Double[count]);

        for (int i = 0; i < count; i++)
            value[i] = new Matrix3Double();
    }

    // Utility \\

    @Override
    protected void push(int handle, Matrix3Double[] value) {

    }
}
