package com.AdventureRPG.core.shaderpipeline.uniform.vectors;

import com.AdventureRPG.core.shaderpipeline.uniform.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Vectors.Vector3Int;

public class Vector3IntUniform extends UniformAttribute<Vector3Int> {

    // Base \\

    public Vector3IntUniform() {
        super(new Vector3Int());
    }

    // Utility \\

    @Override
    protected void push(int handle, Vector3Int value) {

    }
}
