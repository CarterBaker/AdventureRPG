package com.internal.bootstrap.shaderpipeline.uniforms.scalars;

import com.internal.core.app.CoreContext;
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
        CoreContext.gl.glUniform1f(handle, value);
    }

    @Override
    protected void applyValue(Float value) {
        this.value = value;
    }
}
