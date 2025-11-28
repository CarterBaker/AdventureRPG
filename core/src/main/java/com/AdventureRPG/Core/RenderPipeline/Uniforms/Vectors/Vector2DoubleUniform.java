package com.AdventureRPG.Core.RenderPipeline.Uniforms.Vectors;

import com.AdventureRPG.Core.RenderPipeline.Uniforms.UniformAttribute;
import com.AdventureRPG.Core.Util.Methematics.Vectors.Vector2Double;

public class Vector2DoubleUniform extends UniformAttribute<Vector2Double> {

    // Base \\

    public Vector2DoubleUniform() {
        super(new Vector2Double());
    }

    public Vector2DoubleUniform(Vector2Double value) {
        super(value);
    }

    // Utility \\

    @Override
    protected void push(int handle, Vector2Double value) {

    }
}
