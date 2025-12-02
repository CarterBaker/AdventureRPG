package com.AdventureRPG.Core.RenderPipeline.Uniforms.VectorArrays;

import com.AdventureRPG.Core.RenderPipeline.Uniforms.UniformAttribute;
import com.AdventureRPG.Core.Util.Methematics.Vectors.Vector3Boolean;

public class Vector3BooleanArrayUniform extends UniformAttribute<Vector3Boolean[]> {

    // Base \\

    public Vector3BooleanArrayUniform(int count) {

        super(new Vector3Boolean[count]);

        for (int i = 0; i < count; i++)
            value[i] = new Vector3Boolean();
    }

    // Utility \\

    @Override
    protected void push(int handle, Vector3Boolean[] value) {

    }
}
