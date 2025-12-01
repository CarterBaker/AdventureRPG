package com.AdventureRPG.Core.RenderPipeline.Uniforms.Matrices;

import com.AdventureRPG.Core.RenderPipeline.Uniforms.UniformAttribute;
import com.AdventureRPG.Core.Util.Methematics.Matrices.Matrix4Double;

public class Matrix4DoubleUniform extends UniformAttribute<Matrix4Double> {

    // Base \\

    public Matrix4DoubleUniform() {
        super(new Matrix4Double());
    }

    // Utility \\

    @Override
    protected void push(int handle, Matrix4Double value) {

    }
}
