package com.AdventureRPG.Core.RenderPipeline.Uniform.Vectors;

import com.AdventureRPG.Core.RenderPipeline.Uniform.UniformAttribute;
import com.AdventureRPG.Core.Util.Methematics.Vectors.Vector4;

public class Vector4Uniform extends UniformAttribute<Vector4> {

    // Base \\

    public Vector4Uniform() {
        super(new Vector4());
    }

    // Utility \\

    @Override
    protected void push(int handle, Vector4 value) {

    }
}
