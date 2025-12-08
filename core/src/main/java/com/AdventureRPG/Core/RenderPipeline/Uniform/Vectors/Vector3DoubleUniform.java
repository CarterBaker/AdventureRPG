package com.AdventureRPG.core.renderpipeline.uniform.vectors;

import com.AdventureRPG.core.renderpipeline.uniform.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Vectors.Vector3Double;

public class Vector3DoubleUniform extends UniformAttribute<Vector3Double> {

    // Base \\

    public Vector3DoubleUniform() {
        super(new Vector3Double());
    }

    // Utility \\

    @Override
    protected void push(int handle, Vector3Double value) {

    }
}
