package com.AdventureRPG.core.shaderpipeline.uniform.vectors;

import com.AdventureRPG.core.shaderpipeline.uniform.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Vectors.Vector4;

public class Vector4Uniform extends UniformAttribute<Vector4> {

    // Base \\

    public Vector4Uniform() {
        super(new Vector4());
    }

    // Utility \\

    @Override
    protected void push(int handle, Vector4 value) {

    }
}
