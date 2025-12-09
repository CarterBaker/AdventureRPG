package com.AdventureRPG.core.shaderpipeline.uniform.vectors;

import com.AdventureRPG.core.shaderpipeline.uniform.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Vectors.Vector3Boolean;

public class Vector3BooleanUniform extends UniformAttribute<Vector3Boolean> {

    // Base \\

    public Vector3BooleanUniform() {
        super(new Vector3Boolean());
    }

    // Utility \\

    @Override
    protected void push(int handle, Vector3Boolean value) {

    }
}
