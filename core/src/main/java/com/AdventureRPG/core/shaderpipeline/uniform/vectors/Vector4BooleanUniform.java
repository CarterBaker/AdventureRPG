package com.AdventureRPG.core.shaderpipeline.uniform.vectors;

import com.AdventureRPG.core.shaderpipeline.uniform.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Vectors.Vector4Boolean;

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
