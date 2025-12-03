package com.AdventureRPG.Core.RenderPipeline.Uniform.VectorArrays;

import com.AdventureRPG.Core.RenderPipeline.Uniform.UniformAttribute;
import com.AdventureRPG.Core.Util.Methematics.Vectors.Vector4Double;

public class Vector4DoubleArrayUniform extends UniformAttribute<Vector4Double[]> {

    // Base \\

    public Vector4DoubleArrayUniform(int count) {

        super(new Vector4Double[count]);

        for (int i = 0; i < count; i++)
            value[i] = new Vector4Double();
    }

    // Utility \\

    @Override
    protected void push(int handle, Vector4Double[] value) {

    }
}
