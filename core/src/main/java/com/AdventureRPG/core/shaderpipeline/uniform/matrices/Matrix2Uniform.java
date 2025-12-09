package com.AdventureRPG.core.shaderpipeline.uniform.matrices;

import com.AdventureRPG.core.shaderpipeline.uniform.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Matrices.Matrix2;

public class Matrix2Uniform extends UniformAttribute<Matrix2> {

    // Base \\

    public Matrix2Uniform() {
        super(new Matrix2());
    }

    // Utility \\

    @Override
    protected void push(int handle, Matrix2 value) {

    }
}
