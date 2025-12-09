package com.AdventureRPG.core.shaderpipeline.uniform.vectors;

import com.AdventureRPG.core.shaderpipeline.uniform.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Vectors.Vector2Int;

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
