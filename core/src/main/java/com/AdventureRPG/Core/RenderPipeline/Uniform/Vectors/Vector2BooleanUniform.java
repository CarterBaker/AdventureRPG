package com.AdventureRPG.Core.RenderPipeline.Uniform.Vectors;

import com.AdventureRPG.Core.RenderPipeline.Uniform.UniformAttribute;
import com.AdventureRPG.Core.Util.Methematics.Vectors.Vector2Boolean;

public class Vector2BooleanUniform extends UniformAttribute<Vector2Boolean> {

    // Base \\

    public Vector2BooleanUniform() {
        super(new Vector2Boolean());
    }

    // Utility \\

    @Override
    protected void push(int handle, Vector2Boolean value) {

    }
}
