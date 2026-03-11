package com.internal.bootstrap.shaderpipeline.uniforms.vectors;

import com.badlogic.gdx.Gdx;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformType;
import com.internal.core.util.mathematics.vectors.Vector4;

public final class Vector4Uniform extends UniformAttribute<Object> {

    public Vector4Uniform() {
        super(UniformType.VECTOR4, new Vector4());
    }

    @Override
    public UniformAttribute<?> createDefault() {
        return new Vector4Uniform();
    }

    @Override
    protected void push(int handle, Object value) {
        if (value instanceof com.badlogic.gdx.math.Vector4 gdxVector)
            Gdx.gl.glUniform4f(handle, gdxVector.x, gdxVector.y, gdxVector.z, gdxVector.w);
        else if (value instanceof Vector4 internalVector)
            Gdx.gl.glUniform4f(handle, internalVector.x, internalVector.y, internalVector.z, internalVector.w);
        else
            throw new IllegalArgumentException(
                    "push(int, Vector4): Expected Vector4 or com.badlogic.gdx.math.Vector4, got " + value.getClass());
    }

    @Override
    protected void applyValue(Object value) {
        if (value instanceof com.badlogic.gdx.math.Vector4 gdxVector)
            ((Vector4) this.value).fromGDX(gdxVector);
        else if (value instanceof Vector4 internalVector)
            ((Vector4) this.value).set(internalVector);
        else
            throw new IllegalArgumentException(
                    "applyValue(Vector4): Expected Vector4 or com.badlogic.gdx.math.Vector4, got " + value.getClass());
    }
}