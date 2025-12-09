package com.AdventureRPG.core.shaderpipeline.uniform.matrices;

import com.AdventureRPG.core.shaderpipeline.uniform.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Matrices.Matrix2Double;

public class Matrix2DoubleUniform extends UniformAttribute<Matrix2Double> {

    // Base \\

    public Matrix2DoubleUniform() {
        super(new Matrix2Double());
    }

    // Utility \\

    @Override
    protected void push(int handle, Matrix2Double value) {

    }
}
