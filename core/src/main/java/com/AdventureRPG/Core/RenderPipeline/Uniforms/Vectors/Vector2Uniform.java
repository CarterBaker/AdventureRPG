package com.AdventureRPG.Core.RenderPipeline.Uniforms.Vectors;

import com.AdventureRPG.Core.RenderPipeline.Uniforms.UniformAttribute;
import com.AdventureRPG.Core.Util.Methematics.Vectors.Vector2;

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
