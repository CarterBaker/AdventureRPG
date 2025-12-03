package com.AdventureRPG.Core.RenderPipeline.Uniform.MatrixArrays;

import com.AdventureRPG.Core.RenderPipeline.Uniform.UniformAttribute;
import com.AdventureRPG.Core.Util.Methematics.Matrices.Matrix3;

public class Matrix3ArrayUniform extends UniformAttribute<Matrix3[]> {

    // Base \\

    public Matrix3ArrayUniform(int count) {

        super(new Matrix3[count]);

        for (int i = 0; i < count; i++)
            value[i] = new Matrix3();
    }

    // Utility \\

    @Override
    protected void push(int handle, Matrix3[] value) {

    }
}
