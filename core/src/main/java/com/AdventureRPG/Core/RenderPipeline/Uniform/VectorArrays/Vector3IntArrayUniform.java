package com.AdventureRPG.core.renderpipeline.uniform.vectorarrays;

import com.AdventureRPG.core.renderpipeline.uniform.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Vectors.Vector3Int;

public class Vector3IntArrayUniform extends UniformAttribute<Vector3Int[]> {

    // Base \\

    public Vector3IntArrayUniform(int count) {

        super(new Vector3Int[count]);

        for (int i = 0; i < count; i++)
            value[i] = new Vector3Int();
    }

    // Utility \\

    @Override
    protected void push(int handle, Vector3Int[] value) {

    }
}
