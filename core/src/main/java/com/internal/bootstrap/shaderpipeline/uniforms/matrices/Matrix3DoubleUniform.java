package com.internal.bootstrap.shaderpipeline.uniforms.matrices;

import com.internal.platform.Gdx;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformType;
import com.internal.core.util.mathematics.matrices.Matrix3;
import com.internal.core.util.mathematics.matrices.Matrix3Double;

public final class Matrix3DoubleUniform extends UniformAttributeStruct<Matrix3Double> {

    private final Matrix3 uniformBuffer;

    public Matrix3DoubleUniform() {
        super(UniformType.MATRIX3_DOUBLE, new Matrix3Double());
        this.uniformBuffer = new Matrix3();
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Matrix3DoubleUniform();
    }

    @Override
    protected void push(int handle, Matrix3Double value) {
        for (int i = 0; i < 9; i++)
            uniformBuffer.val[i] = (float) value.val[i];
        Gdx.gl.glUniformMatrix3fv(handle, 1, false, uniformBuffer.val, 0);
    }

    @Override
    protected void applyValue(Matrix3Double value) {
        this.value.set(value);
    }
}