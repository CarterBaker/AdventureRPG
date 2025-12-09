package com.AdventureRPG.core.shaderpipeline.uniform.vectors;

import com.AdventureRPG.core.shaderpipeline.uniform.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Vectors.Vector2Double;

public class Vector2DoubleUniform extends UniformAttribute<Vector2Double> {

    // Base \\

    public Vector2DoubleUniform() {
        super(new Vector2Double());
    }

    // Utility \\

    @Override
    protected void push(int handle, Vector2Double value) {

    }
}
