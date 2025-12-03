package com.AdventureRPG.Core.RenderPipeline.Uniform.VectorArrays;

import com.AdventureRPG.Core.RenderPipeline.Uniform.UniformAttribute;
import com.AdventureRPG.Core.Util.Methematics.Vectors.Vector2Int;

public class Vector2IntArrayUniform extends UniformAttribute<Vector2Int[]> {

    // Base \\

    public Vector2IntArrayUniform(int count) {

        super(new Vector2Int[count]);

        for (int i = 0; i < count; i++)
            value[i] = new Vector2Int();
    }

    // Utility \\

    @Override
    protected void push(int handle, Vector2Int[] value) {

    }
}
