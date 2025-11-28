package com.AdventureRPG.Core.RenderPipeline.Uniforms.Matrices;

import com.AdventureRPG.Core.RenderPipeline.Uniforms.UniformAttribute;
import com.AdventureRPG.Core.Util.Methematics.Matrices.Matrix4;

public class Matrix4Uniform extends UniformAttribute<Matrix4> {

    // Base \\

    public Matrix4Uniform() {
        super(new Matrix4());
    }

    public Matrix4Uniform(Matrix4 value) {
        super(value);
    }

    // Utility \\

    @Override
    protected void push(int handle, Matrix4 value) {

    }
}
