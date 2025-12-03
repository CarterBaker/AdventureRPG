package com.AdventureRPG.Core.RenderPipeline.Uniform.Vectors;

import com.AdventureRPG.Core.RenderPipeline.Uniform.UniformAttribute;
import com.AdventureRPG.Core.Util.Methematics.Vectors.Vector3Int;

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
