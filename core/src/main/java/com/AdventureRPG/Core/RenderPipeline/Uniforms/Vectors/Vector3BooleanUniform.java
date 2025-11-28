package com.AdventureRPG.Core.RenderPipeline.Uniforms.Vectors;

import com.AdventureRPG.Core.RenderPipeline.Uniforms.UniformAttribute;
import com.AdventureRPG.Core.Util.Methematics.Vectors.Vector3Boolean;

public class Vector3BooleanUniform extends UniformAttribute<Vector3Boolean> {

    // Base \\

    public Vector3BooleanUniform() {
        super(new Vector3Boolean());
    }

    public Vector3BooleanUniform(Vector3Boolean value) {
        super(value);
    }

    // Utility \\

    @Override
    protected void push(int handle, Vector3Boolean value) {

    }
}
