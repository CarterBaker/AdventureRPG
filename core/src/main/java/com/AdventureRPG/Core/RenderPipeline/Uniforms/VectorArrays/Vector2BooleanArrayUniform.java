package com.AdventureRPG.Core.RenderPipeline.Uniforms.VectorArrays;

import com.AdventureRPG.Core.RenderPipeline.Uniforms.UniformAttribute;
import com.AdventureRPG.Core.Util.Methematics.Vectors.Vector2Boolean;

public class Vector2BooleanArrayUniform extends UniformAttribute<Vector2Boolean[]> {

    // Base \\

    public Vector2BooleanArrayUniform(int count) {

        super(new Vector2Boolean[count]);

        for (int i = 0; i < count; i++)
            value[i] = new Vector2Boolean();
    }

    // Utility \\

    @Override
    protected void push(int handle, Vector2Boolean[] value) {

    }
}
