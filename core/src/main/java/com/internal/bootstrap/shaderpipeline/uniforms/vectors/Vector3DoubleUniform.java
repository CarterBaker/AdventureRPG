package com.internal.bootstrap.shaderpipeline.uniforms.vectors;

import com.badlogic.gdx.Gdx;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformType;
import com.internal.core.util.mathematics.vectors.Vector3Double;

public final class Vector3DoubleUniform extends UniformAttribute<Vector3Double> {

    public Vector3DoubleUniform() {
        super(UniformType.VECTOR3_DOUBLE, new Vector3Double());
    }

    @Override
    public UniformAttribute<?> createDefault() {
        return new Vector3DoubleUniform();
    }

    @Override
    protected void push(int handle, Vector3Double value) {
        Gdx.gl.glUniform3f(handle, (float) value.x, (float) value.y, (float) value.z);
    }

    @Override
    protected void applyValue(Vector3Double value) {
        this.value.set(value);
    }
}