package com.internal.bootstrap.shaderpipeline.uniforms.matrices;

import com.badlogic.gdx.Gdx;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformType;
import com.internal.core.util.mathematics.matrices.Matrix4;
import com.internal.core.util.mathematics.matrices.Matrix4Double;

public final class Matrix4DoubleUniform extends UniformAttribute<Matrix4Double> {

    private final Matrix4 uniformBuffer;

    public Matrix4DoubleUniform() {
        super(UniformType.MATRIX4_DOUBLE, new Matrix4Double());
        this.uniformBuffer = new Matrix4();
    }

    @Override
    public UniformAttribute<?> createDefault() {
        return new Matrix4DoubleUniform();
    }

    @Override
    protected void push(int handle, Matrix4Double value) {
        for (int i = 0; i < 16; i++)
            uniformBuffer.val[i] = (float) value.val[i];
        Gdx.gl.glUniformMatrix4fv(handle, 1, false, uniformBuffer.val, 0);
    }

    @Override
    protected void applyValue(Matrix4Double value) {
        this.value.set(value);
    }
}