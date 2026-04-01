package com.internal.bootstrap.shaderpipeline.uniforms.vectors;

import com.internal.core.app.CoreContext;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformType;
import com.internal.core.util.mathematics.vectors.Vector3Int;

public final class Vector3IntUniform extends UniformAttributeStruct<Vector3Int> {

    public Vector3IntUniform() {
        super(UniformType.VECTOR3_INT, new Vector3Int());
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector3IntUniform();
    }

    @Override
    protected void push(int handle, Vector3Int value) {
        CoreContext.gl.glUniform3i(handle, value.x, value.y, value.z);
    }

    @Override
    protected void applyValue(Vector3Int value) {
        this.value.set(value);
    }
}
