package com.internal.bootstrap.shaderpipeline.uniforms.matrices;

import com.internal.platform.PlatformRuntime;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformType;
import com.internal.core.util.mathematics.matrices.Matrix4;

public final class Matrix4Uniform extends UniformAttributeStruct<Object> {

    public Matrix4Uniform() {
        super(UniformType.MATRIX4, new Matrix4());
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Matrix4Uniform();
    }

    @Override
    protected void push(int handle, Object value) {
        if (value instanceof Matrix4 m)
            PlatformRuntime.gl.glUniformMatrix4fv(handle, 1, false, m.val, 0);
        else
            throw new IllegalArgumentException("push(Matrix4): got " + value.getClass());
    }

    @Override
    protected void applyValue(Object value) {
        if (value instanceof Matrix4 m)
            ((Matrix4) this.value).set(m);
        else
            throw new IllegalArgumentException("applyValue(Matrix4): got " + value.getClass());
    }

    @Override
    protected void applyObject(Object value) {
        applyValue(value);
    }
}
