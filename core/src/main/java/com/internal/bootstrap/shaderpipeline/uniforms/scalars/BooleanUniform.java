package com.internal.bootstrap.shaderpipeline.uniforms.scalars;

import com.badlogic.gdx.Gdx;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformType;

public final class BooleanUniform extends UniformAttribute<Boolean> {

    public BooleanUniform() {
        super(UniformType.BOOL, false);
    }

    @Override
    public UniformAttribute<?> createDefault() {
        return new BooleanUniform();
    }

    @Override
    protected void push(int handle, Boolean value) {
        Gdx.gl.glUniform1i(handle, value ? 1 : 0);
    }

    @Override
    protected void applyValue(Boolean value) {
        this.value = value;
    }
}