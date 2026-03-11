package com.internal.bootstrap.shaderpipeline.uniforms.scalars;

import com.badlogic.gdx.Gdx;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformType;

public final class FloatUniform extends UniformAttribute<Float> {

    public FloatUniform() {
        super(UniformType.FLOAT, 0f);
    }

    @Override
    public UniformAttribute<?> createDefault() {
        return new FloatUniform();
    }

    @Override
    protected void push(int handle, Float value) {
        Gdx.gl.glUniform1f(handle, value);
    }

    @Override
    protected void applyValue(Float value) {
        this.value = value;
    }
}