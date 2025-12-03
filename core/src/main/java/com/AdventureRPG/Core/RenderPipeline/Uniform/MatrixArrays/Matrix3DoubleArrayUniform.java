package com.AdventureRPG.Core.RenderPipeline.Uniform.MatrixArrays;

import com.AdventureRPG.Core.RenderPipeline.Uniform.UniformAttribute;
import com.AdventureRPG.Core.Util.Methematics.Matrices.Matrix3Double;

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
