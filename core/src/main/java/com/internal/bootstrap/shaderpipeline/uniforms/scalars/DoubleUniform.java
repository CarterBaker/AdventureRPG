package com.internal.bootstrap.shaderpipeline.uniforms.scalars;

import com.internal.platform.Gdx;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformType;

public final class DoubleUniform extends UniformAttributeStruct<Double> {

    public DoubleUniform() {
        super(UniformType.DOUBLE, 0.0);
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
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