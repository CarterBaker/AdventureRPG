package com.AdventureRPG.Core.RenderPipeline.Uniforms.Vectors;

import com.AdventureRPG.Core.RenderPipeline.Uniforms.UniformAttribute;
import com.AdventureRPG.Core.Util.Methematics.Vectors.Vector4Int;

public class Vector4IntUniform extends UniformAttribute<Vector4Int> {

    // Base \\

    public Vector4IntUniform() {
        super(new Vector4Int());
    }

    public Vector4IntUniform(Vector4Int value) {
        super(value);
    }

    // Utility \\

    @Override
    protected void push(int handle, Vector4Int value) {

    }
}
