package com.internal.bootstrap.shaderpipeline.uniforms.vectors;

import com.internal.platform.PlatformRuntime;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformType;
import com.internal.core.util.mathematics.vectors.Vector4Int;

public final class Vector4IntUniform extends UniformAttributeStruct<Vector4Int> {

    public Vector4IntUniform() {
        super(UniformType.VECTOR4_INT, new Vector4Int());
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector4IntUniform();
    }

    @Override
    protected void push(int handle, Vector4Int value) {
        PlatformRuntime.gl.glUniform4i(handle, value.x, value.y, value.z, value.w);
    }

    @Override
    protected void applyValue(Vector4Int value) {
        this.value.set(value);
    }
}
