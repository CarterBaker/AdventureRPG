package com.AdventureRPG.Core.RenderPipeline.Uniform.Vectors;

import com.AdventureRPG.Core.RenderPipeline.Uniform.UniformAttribute;
import com.AdventureRPG.Core.Util.Methematics.Vectors.Vector4Boolean;

public class Vector4BooleanUniform extends UniformAttribute<Vector4Boolean> {

    // Base \\

    public Vector4BooleanUniform() {
        super(new Vector4Boolean());
    }

    // Utility \\

    @Override
    protected void push(int handle, Vector4Boolean value) {

    }
}
