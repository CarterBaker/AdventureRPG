package com.AdventureRPG.core.renderpipeline.uniform.matrices;

import com.AdventureRPG.core.renderpipeline.uniform.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Matrices.Matrix4Double;

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
