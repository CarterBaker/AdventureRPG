package com.AdventureRPG.Core.RenderPipeline.Uniforms.Vectors;

import com.AdventureRPG.Core.RenderPipeline.Uniforms.UniformAttribute;
import com.AdventureRPG.Core.Util.Methematics.Vectors.Vector4;

public class Vector4Uniform extends UniformAttribute<Vector4> {

    // Base \\

    public Vector4Uniform() {
        super(new Vector4());
    }

    public Vector4Uniform(Vector4 value) {
        super(value);
    }

    // Utility \\

    @Override
    protected void push(int handle, Vector4 value) {

    }
}
