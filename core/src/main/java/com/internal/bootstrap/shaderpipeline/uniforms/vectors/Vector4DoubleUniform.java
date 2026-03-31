package com.internal.bootstrap.shaderpipeline.uniforms.vectors;

import com.internal.platform.Gdx;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformType;
import com.internal.core.util.mathematics.vectors.Vector4Double;

public final class Vector4DoubleUniform extends UniformAttributeStruct<Vector4Double> {

    public Vector4DoubleUniform() {
        super(UniformType.VECTOR4_DOUBLE, new Vector4Double());
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector4DoubleUniform();
    }

    @Override
    protected void push(int handle, Vector4Double value) {
        Gdx.gl.glUniform4f(handle, (float) value.x, (float) value.y, (float) value.z, (float) value.w);
    }

    @Override
    protected void applyValue(Vector4Double value) {
        this.value.set(value);
    }
}