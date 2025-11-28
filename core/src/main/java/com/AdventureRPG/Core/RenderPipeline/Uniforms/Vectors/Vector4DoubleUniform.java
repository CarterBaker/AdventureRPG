package com.AdventureRPG.Core.RenderPipeline.Uniforms.Vectors;

import com.AdventureRPG.Core.RenderPipeline.Uniforms.UniformAttribute;
import com.AdventureRPG.Core.Util.Methematics.Vectors.Vector4Double;

public class Vector4DoubleUniform extends UniformAttribute<Vector4Double> {

    // Base \\

    public Vector4DoubleUniform() {
        super(new Vector4Double());
    }

    public Vector4DoubleUniform(Vector4Double value) {
        super(value);
    }

    // Utility \\

    @Override
    protected void push(int handle, Vector4Double value) {

    }
}
