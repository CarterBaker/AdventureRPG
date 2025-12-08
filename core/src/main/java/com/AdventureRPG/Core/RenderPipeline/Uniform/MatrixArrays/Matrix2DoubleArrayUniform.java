package com.AdventureRPG.core.renderpipeline.uniform.matrixArrays;

import com.AdventureRPG.core.renderpipeline.uniform.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Matrices.Matrix2Double;

public class Matrix2DoubleArrayUniform extends UniformAttribute<Matrix2Double[]> {

    // Base \\

    public Matrix2DoubleArrayUniform(int count) {

        super(new Matrix2Double[count]);

        for (int i = 0; i < count; i++)
            value[i] = new Matrix2Double();
    }

    // Utility \\

    @Override
    protected void push(int handle, Matrix2Double[] value) {

    }
}
