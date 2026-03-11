package com.internal.bootstrap.shaderpipeline.uniforms.vectors;

import com.badlogic.gdx.Gdx;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformType;
import com.internal.core.util.mathematics.vectors.Vector3;

public final class Vector3Uniform extends UniformAttribute<Object> {

    public Vector3Uniform() {
        super(UniformType.VECTOR3, new Vector3());
    }

    @Override
    public UniformAttribute<?> createDefault() {
        return new Vector3Uniform();
    }

    @Override
    protected void push(int handle, Object value) {
        if (value instanceof com.badlogic.gdx.math.Vector3 gdxVector)
            Gdx.gl.glUniform3f(handle, gdxVector.x, gdxVector.y, gdxVector.z);
        else if (value instanceof Vector3 internalVector)
            Gdx.gl.glUniform3f(handle, internalVector.x, internalVector.y, internalVector.z);
        else
            throw new IllegalArgumentException(
                    "push(int, Vector3): Expected Vector3 or com.badlogic.gdx.math.Vector3, got " + value.getClass());
    }

    @Override
    protected void applyValue(Object value) {
        if (value instanceof com.badlogic.gdx.math.Vector3 gdxVector)
            ((Vector3) this.value).fromGDX(gdxVector);
        else if (value instanceof Vector3 internalVector)
            ((Vector3) this.value).set(internalVector);
        else
            throw new IllegalArgumentException(
                    "applyValue(Vector3): Expected Vector3 or com.badlogic.gdx.math.Vector3, got " + value.getClass());
    }
}