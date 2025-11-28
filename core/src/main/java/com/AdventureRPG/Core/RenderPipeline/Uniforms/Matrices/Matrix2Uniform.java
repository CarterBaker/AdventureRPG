package com.AdventureRPG.Core.RenderPipeline.Uniforms.Matrices;

import com.AdventureRPG.Core.RenderPipeline.Uniforms.UniformAttribute;
import com.AdventureRPG.Core.Util.Methematics.Matrices.Matrix2;

public class Matrix2Uniform extends UniformAttribute<Matrix2> {

    // Base \\

    public Matrix2Uniform() {
        super(new Matrix2());
    }

    public Matrix2Uniform(Matrix2 value) {
        super(value);
    }

    // Utility \\

    @Override
    protected void push(int handle, Matrix2 value) {

    }
}
