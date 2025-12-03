package com.AdventureRPG.Core.RenderPipeline.Uniform.Vectors;

import com.AdventureRPG.Core.RenderPipeline.Uniform.UniformAttribute;
import com.AdventureRPG.Core.Util.Methematics.Vectors.Vector2Double;

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
