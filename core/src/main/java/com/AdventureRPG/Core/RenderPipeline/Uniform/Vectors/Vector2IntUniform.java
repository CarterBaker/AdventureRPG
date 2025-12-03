package com.AdventureRPG.Core.RenderPipeline.Uniform.Vectors;

import com.AdventureRPG.Core.RenderPipeline.Uniform.UniformAttribute;
import com.AdventureRPG.Core.Util.Methematics.Vectors.Vector2Int;

public class Vector2IntUniform extends UniformAttribute<Vector2Int> {

    // Base \\

    public Vector2IntUniform() {
        super(new Vector2Int());
    }

    // Utility \\

    @Override
    protected void push(int handle, Vector2Int value) {

    }
}
