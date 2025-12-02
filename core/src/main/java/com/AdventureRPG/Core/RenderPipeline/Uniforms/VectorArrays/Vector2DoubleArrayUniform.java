package com.AdventureRPG.Core.RenderPipeline.Uniforms.VectorArrays;

import com.AdventureRPG.Core.RenderPipeline.Uniforms.UniformAttribute;
import com.AdventureRPG.Core.Util.Methematics.Vectors.Vector2Double;

public class Vector2DoubleArrayUniform extends UniformAttribute<Vector2Double[]> {

    // Base \\

    public Vector2DoubleArrayUniform(int count) {

        super(new Vector2Double[count]);

        for (int i = 0; i < count; i++)
            value[i] = new Vector2Double();
    }

    // Utility \\

    @Override
    protected void push(int handle, Vector2Double[] value) {

    }
}
