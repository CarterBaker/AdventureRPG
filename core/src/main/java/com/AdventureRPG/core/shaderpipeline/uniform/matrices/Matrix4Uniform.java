package com.AdventureRPG.core.shaderpipeline.uniform.matrices;

import com.AdventureRPG.core.shaderpipeline.uniform.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Matrices.Matrix4;

public class Matrix4Uniform extends UniformAttribute<Matrix4> {

    // Base \\

    public Matrix4Uniform() {
        super(new Matrix4());
    }

    // Utility \\

    @Override
    protected void push(int handle, Matrix4 value) {

    }
}
