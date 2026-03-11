package com.internal.bootstrap.shaderpipeline.uniforms.matrices;

import com.badlogic.gdx.Gdx;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformType;
import com.internal.core.util.mathematics.matrices.Matrix2Double;

import java.nio.FloatBuffer;

public final class Matrix2DoubleUniform extends UniformAttribute<Matrix2Double> {

    private final FloatBuffer uniformBuffer;

    public Matrix2DoubleUniform() {
        super(UniformType.MATRIX2_DOUBLE, new Matrix2Double());
        this.uniformBuffer = uboBuffer.asFloatBuffer();
    }

    @Override
    public UniformAttribute<?> createDefault() {
        return new Matrix2DoubleUniform();
    }

    @Override
    protected void push(int handle, Matrix2Double value) {
        uniformBuffer.clear();
        uniformBuffer.put((float) value.val[0]);
        uniformBuffer.put((float) value.val[1]);
        uniformBuffer.put((float) value.val[2]);
        uniformBuffer.put((float) value.val[3]);
        uniformBuffer.flip();
        Gdx.gl.glUniformMatrix2fv(handle, 1, false, uniformBuffer);
    }

    @Override
    protected void applyValue(Matrix2Double value) {
        this.value.set(value);
    }
}