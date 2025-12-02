package com.AdventureRPG.Core.RenderPipeline.Uniforms.MatrixArrays;

import com.AdventureRPG.Core.RenderPipeline.Uniforms.UniformAttribute;
import com.AdventureRPG.Core.Util.Methematics.Matrices.Matrix4;

public class Matrix4ArrayUniform extends UniformAttribute<Matrix4[]> {

    // Base \\

    public Matrix4ArrayUniform(int count) {

        super(new Matrix4[count]);

        for (int i = 0; i < count; i++)
            value[i] = new Matrix4();
    }

    // Utility \\

    @Override
    protected void push(int handle, Matrix4[] value) {

    }
}
