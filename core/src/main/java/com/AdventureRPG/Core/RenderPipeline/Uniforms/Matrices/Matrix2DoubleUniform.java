package com.AdventureRPG.Core.RenderPipeline.Uniforms.Matrices;

import com.AdventureRPG.Core.RenderPipeline.Uniforms.UniformAttribute;
import com.AdventureRPG.Core.Util.Methematics.Matrices.Matrix2Double;

public class Matrix2DoubleUniform extends UniformAttribute<Matrix2Double> {

    // Base \\

    public Matrix2DoubleUniform() {
        super(new Matrix2Double());
    }

    public Matrix2DoubleUniform(Matrix2Double value) {
        super(value);
    }

    // Utility \\

    @Override
    protected void push(int handle, Matrix2Double value) {

    }
}
