package com.AdventureRPG.core.renderpipeline.uniform.vectors;

import com.AdventureRPG.core.renderpipeline.uniform.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Vectors.Vector4Int;

public class Vector4IntUniform extends UniformAttribute<Vector4Int> {

    // Base \\

    public Vector4IntUniform() {
        super(new Vector4Int());
    }

    // Utility \\

    @Override
    protected void push(int handle, Vector4Int value) {

    }
}
