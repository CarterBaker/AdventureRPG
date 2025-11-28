package com.AdventureRPG.Core.RenderPipeline.Uniforms.Vectors;

import com.AdventureRPG.Core.RenderPipeline.Uniforms.UniformAttribute;
import com.AdventureRPG.Core.Util.Methematics.Vectors.Vector3;

public class Vector3Uniform extends UniformAttribute<Vector3> {

    // Base \\

    public Vector3Uniform() {
        super(new Vector3());
    }

    public Vector3Uniform(Vector3 value) {
        super(value);
    }

    // Utility \\

    @Override
    protected void push(int handle, Vector3 value) {

    }
}
