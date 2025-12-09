package com.AdventureRPG.core.shaderpipeline.uniform.vectors;

import com.AdventureRPG.core.shaderpipeline.uniform.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Vectors.Vector2;

public class Vector2Uniform extends UniformAttribute<Vector2> {

    // Base \\

    public Vector2Uniform() {
        super(new Vector2());
    }

    // Utility \\

    @Override
    protected void push(int handle, Vector2 value) {

    }
}
