package com.internal.bootstrap.shaderpipeline.uniforms.matrices;

import com.badlogic.gdx.Gdx;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformType;
import com.internal.core.util.mathematics.matrices.Matrix3;

public final class Matrix3Uniform extends UniformAttribute<Object> {

    public Matrix3Uniform() {
        super(UniformType.MATRIX3, new Matrix3());
    }

    @Override
    public UniformAttribute<?> createDefault() {
        return new Matrix3Uniform();
    }

    @Override
    protected void push(int handle, Object value) {
        if (value instanceof com.badlogic.gdx.math.Matrix3 m)
            Gdx.gl.glUniformMatrix3fv(handle, 1, false, m.val, 0);
        else if (value instanceof Matrix3 m)
            Gdx.gl.glUniformMatrix3fv(handle, 1, false, m.val, 0);
        else
            throw new IllegalArgumentException("push(Matrix3): got " + value.getClass());
    }

    @Override
    protected void applyValue(Object value) {
        Matrix3 target = (Matrix3) this.value;
        if (value instanceof com.badlogic.gdx.math.Matrix3 m)
            target.fromGDX(m);
        else if (value instanceof Matrix3 m)
            target.set(m);
        else
            throw new IllegalArgumentException("applyValue(Matrix3): got " + value.getClass());
    }

    @Override
    protected void applyObject(Object value) {
        applyValue(value);
    }
}