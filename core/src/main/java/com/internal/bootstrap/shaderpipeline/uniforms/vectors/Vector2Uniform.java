package com.internal.bootstrap.shaderpipeline.uniforms.vectors;

import com.badlogic.gdx.Gdx;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformType;
import com.internal.core.util.mathematics.vectors.Vector2;

public final class Vector2Uniform extends UniformAttributeStruct<Object> {

    public Vector2Uniform() {
        super(UniformType.VECTOR2, new Vector2());
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector2Uniform();
    }

    @Override
    protected void push(int handle, Object value) {
        if (value instanceof com.badlogic.gdx.math.Vector2 gdxVector)
            Gdx.gl.glUniform2f(handle, gdxVector.x, gdxVector.y);
        else if (value instanceof Vector2 internalVector)
            Gdx.gl.glUniform2f(handle, internalVector.x, internalVector.y);
        else
            throw new IllegalArgumentException(
                    "push(int, Vector2): Expected Vector2 or com.badlogic.gdx.math.Vector2, got " + value.getClass());
    }

    @Override
    protected void applyValue(Object value) {
        if (value instanceof com.badlogic.gdx.math.Vector2 gdxVector)
            ((Vector2) this.value).fromGDX(gdxVector);
        else if (value instanceof Vector2 internalVector)
            ((Vector2) this.value).set(internalVector);
        else
            throw new IllegalArgumentException(
                    "applyValue(Vector2): Expected Vector2 or com.badlogic.gdx.math.Vector2, got " + value.getClass());
    }
}