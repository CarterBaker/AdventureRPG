package com.AdventureRPG.Core.RenderPipeline.Uniform.VectorArrays;

import com.AdventureRPG.Core.RenderPipeline.Uniform.UniformAttribute;
import com.AdventureRPG.Core.Util.Methematics.Vectors.Vector4Int;

public class Vector4IntArrayUniform extends UniformAttribute<Vector4Int[]> {

    // Base \\

    public Vector4IntArrayUniform(int count) {

        super(new Vector4Int[count]);

        for (int i = 0; i < count; i++)
            value[i] = new Vector4Int();
    }

    // Utility \\

    @Override
    protected void push(int handle, Vector4Int[] value) {

    }
}
