package com.AdventureRPG.Core.RenderPipeline.Uniforms.VectorArrays;

import com.AdventureRPG.Core.RenderPipeline.Uniforms.UniformAttribute;
import com.AdventureRPG.Core.Util.Methematics.Vectors.Vector4Boolean;

public class Vector4BooleanArrayUniform extends UniformAttribute<Vector4Boolean[]> {

    // Base \\

    public Vector4BooleanArrayUniform(int count) {

        super(new Vector4Boolean[count]);

        for (int i = 0; i < count; i++)
            value[i] = new Vector4Boolean();
    }

    // Utility \\

    @Override
    protected void push(int handle, Vector4Boolean[] value) {

    }
}
