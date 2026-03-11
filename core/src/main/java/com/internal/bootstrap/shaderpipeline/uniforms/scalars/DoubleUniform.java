package com.internal.bootstrap.shaderpipeline.uniforms.scalars;

import com.badlogic.gdx.Gdx;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformType;

public final class DoubleUniform extends UniformAttribute<Double> {

    public DoubleUniform() {
        super(UniformType.DOUBLE, 0.0);
    }

    @Override
    public UniformAttribute<?> createDefault() {
        return new DoubleUniform();
    }

    @Override
    protected void push(int handle, Double value) {
        Gdx.gl.glUniform1f(handle, value.floatValue());
    }

    @Override
    protected void applyValue(Double value) {
        this.value = value;
    }
}