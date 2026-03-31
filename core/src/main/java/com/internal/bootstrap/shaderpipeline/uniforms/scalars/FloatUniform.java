package com.internal.bootstrap.shaderpipeline.uniforms.scalars;

import com.internal.platform.Gdx;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformType;

public final class FloatUniform extends UniformAttributeStruct<Float> {

    public FloatUniform() {
        super(UniformType.FLOAT, 0f);
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
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