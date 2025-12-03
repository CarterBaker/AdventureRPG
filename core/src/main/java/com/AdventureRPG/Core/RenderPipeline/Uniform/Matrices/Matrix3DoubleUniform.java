package com.AdventureRPG.Core.RenderPipeline.Uniform.Matrices;

import com.AdventureRPG.Core.RenderPipeline.Uniform.UniformAttribute;
import com.AdventureRPG.Core.Util.Methematics.Matrices.Matrix3Double;

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
