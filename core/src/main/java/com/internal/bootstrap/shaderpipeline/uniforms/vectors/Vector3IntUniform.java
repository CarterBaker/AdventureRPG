package com.internal.bootstrap.shaderpipeline.uniforms.vectors;

import com.badlogic.gdx.Gdx;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformType;
import com.internal.core.util.mathematics.vectors.Vector3Int;

public final class Vector3IntUniform extends UniformAttribute<Vector3Int> {

    public Vector3IntUniform() {
        super(UniformType.VECTOR3_INT, new Vector3Int());
    }

    @Override
    public UniformAttribute<?> createDefault() {
        return new Vector3IntUniform();
    }

    @Override
    protected void push(int handle, Vector3Int value) {
        Gdx.gl.glUniform3i(handle, value.x, value.y, value.z);
    }

    @Override
    protected void applyValue(Vector3Int value) {
        this.value.set(value);
    }
}