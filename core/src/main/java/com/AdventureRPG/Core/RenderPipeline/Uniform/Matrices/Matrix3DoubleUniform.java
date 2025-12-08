package com.AdventureRPG.core.renderpipeline.uniform.matrices;

import com.AdventureRPG.core.renderpipeline.uniform.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Matrices.Matrix3Double;

public class Matrix3DoubleUniform extends UniformAttribute<Matrix3Double> {

    // Base \\

    public Matrix3DoubleUniform() {
        super(new Matrix3Double());
    }

    // Utility \\

    @Override
    protected void push(int handle, Matrix3Double value) {

    }
}
