package com.AdventureRPG.core.shaderpipeline.uniform.vectors;

import com.AdventureRPG.core.shaderpipeline.uniform.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Vectors.Vector4Double;

public class Vector4DoubleUniform extends UniformAttribute<Vector4Double> {

    // Base \\

    public Vector4DoubleUniform() {
        super(new Vector4Double());
    }

    // Utility \\

    @Override
    protected void push(int handle, Vector4Double value) {

    }
}
