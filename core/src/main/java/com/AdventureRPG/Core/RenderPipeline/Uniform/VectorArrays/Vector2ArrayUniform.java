package com.AdventureRPG.core.renderpipeline.uniform.vectorarrays;

import com.AdventureRPG.core.renderpipeline.uniform.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Vectors.Vector2;

public class Vector2ArrayUniform extends UniformAttribute<Vector2[]> {

    // Base \\

    public Vector2ArrayUniform(int count) {

        super(new Vector2[count]);

        for (int i = 0; i < count; i++)
            value[i] = new Vector2();
    }

    // Utility \\

    @Override
    protected void push(int handle, Vector2[] value) {

    }
}
