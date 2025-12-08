package com.AdventureRPG.core.renderpipeline.uniform.vectors;

import com.AdventureRPG.core.renderpipeline.uniform.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Vectors.Vector2Boolean;

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
