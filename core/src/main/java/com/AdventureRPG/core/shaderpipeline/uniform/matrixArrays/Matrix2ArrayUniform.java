package com.AdventureRPG.core.shaderpipeline.uniform.matrixArrays;

import com.AdventureRPG.core.shaderpipeline.uniform.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Matrices.Matrix2;

public class Matrix2ArrayUniform extends UniformAttribute<Matrix2[]> {

    // Base \\

    public Matrix2ArrayUniform(int count) {

        super(new Matrix2[count]);

        for (int i = 0; i < count; i++)
            value[i] = new Matrix2();
    }

    // Utility \\

    @Override
    protected void push(int handle, Matrix2[] value) {

    }
}
