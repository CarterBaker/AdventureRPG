package com.internal.bootstrap.shaderpipeline.uniforms.scalars;

import com.internal.platform.Gdx;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformType;

public final class IntegerUniform extends UniformAttributeStruct<Integer> {

    public IntegerUniform() {
        super(UniformType.INT, 0);
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new IntegerUniform();
    }

    @Override
    protected void push(int handle, Integer value) {
        Gdx.gl.glUniform1i(handle, value);
    }

    @Override
    protected void applyValue(Integer value) {
        this.value = value;
    }
}