package com.AdventureRPG.core.renderpipeline.uniform.matrices;

import com.AdventureRPG.core.renderpipeline.uniform.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Matrices.Matrix3;

public class Matrix3Uniform extends UniformAttribute<Matrix3> {

    // Base \\

    public Matrix3Uniform() {
        super(new Matrix3());
    }

    // Utility \\

    @Override
    protected void push(int handle, Matrix3 value) {

    }
}
