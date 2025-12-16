package com.AdventureRPG.core.shaderpipeline.uniforms.matrixArrays;

import com.AdventureRPG.core.shaderpipeline.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Matrices.Matrix4;

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
