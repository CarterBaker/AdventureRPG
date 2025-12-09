package com.AdventureRPG.core.shaderpipeline.uniform.vectors;

import com.AdventureRPG.core.shaderpipeline.uniform.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Vectors.Vector3;

public class Vector3Uniform extends UniformAttribute<Vector3> {

    // Base \\

    public Vector3Uniform() {
        super(new Vector3());
    }

    // Utility \\

    @Override
    protected void push(int handle, Vector3 value) {

    }
}
