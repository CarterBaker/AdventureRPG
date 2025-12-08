package com.AdventureRPG.core.renderpipeline.uniform.vectorarrays;

import com.AdventureRPG.core.renderpipeline.uniform.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Vectors.Vector2Boolean;

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
