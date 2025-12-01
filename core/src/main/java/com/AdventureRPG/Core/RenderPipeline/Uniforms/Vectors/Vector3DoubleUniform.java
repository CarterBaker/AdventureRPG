package com.AdventureRPG.Core.RenderPipeline.Uniforms.Vectors;

import com.AdventureRPG.Core.RenderPipeline.Uniforms.UniformAttribute;
import com.AdventureRPG.Core.Util.Methematics.Vectors.Vector3Double;

public class Vector3DoubleUniform extends UniformAttribute<Vector3Double> {

    // Base \\

    public Vector3DoubleUniform() {
        super(new Vector3Double());
    }

    // Utility \\

    @Override
    protected void push(int handle, Vector3Double value) {

    }
}
