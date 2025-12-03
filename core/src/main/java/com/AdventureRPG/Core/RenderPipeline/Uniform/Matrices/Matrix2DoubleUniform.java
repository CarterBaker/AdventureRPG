package com.AdventureRPG.Core.RenderPipeline.Uniform.Matrices;

import com.AdventureRPG.Core.RenderPipeline.Uniform.UniformAttribute;
import com.AdventureRPG.Core.Util.Methematics.Matrices.Matrix2Double;

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
