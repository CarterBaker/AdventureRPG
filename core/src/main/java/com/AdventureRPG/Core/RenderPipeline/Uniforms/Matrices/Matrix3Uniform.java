package com.AdventureRPG.Core.RenderPipeline.Uniforms.Matrices;

import com.AdventureRPG.Core.RenderPipeline.Uniforms.UniformAttribute;
import com.AdventureRPG.Core.Util.Methematics.Matrices.Matrix3;

public class Matrix3Uniform extends UniformAttribute<Matrix3> {

    // Base \\

    public Matrix3Uniform() {
        super(new Matrix3());
    }

    public Matrix3Uniform(Matrix3 value) {
        super(value);
    }

    // Utility \\

    @Override
    protected void push(int handle, Matrix3 value) {

    }
}
