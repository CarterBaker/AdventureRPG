package com.internal.bootstrap.shaderpipeline.uniforms.matrixArrays;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformType;
import com.internal.core.util.mathematics.matrices.Matrix3Double;

import java.nio.FloatBuffer;

public final class Matrix3DoubleArrayUniform extends UniformAttribute<Object[]> {

    private final int elementCount;
    private final FloatBuffer uniformBuffer;

    public Matrix3DoubleArrayUniform(int elementCount) {
        super(UniformType.MATRIX3_DOUBLE, elementCount, new Matrix3Double[elementCount]);
        this.elementCount = elementCount;
        this.uniformBuffer = BufferUtils.newFloatBuffer(elementCount * 9);
        for (int i = 0; i < elementCount; i++)
            ((Matrix3Double[]) value)[i] = new Matrix3Double();
    }

    @Override
    public UniformAttribute<?> createDefault() {
        return new Matrix3DoubleArrayUniform(elementCount);
    }

    @Override
    protected void push(int handle, Object[] value) {
        uniformBuffer.clear();
        for (int i = 0; i < elementCount; i++) {
            Matrix3Double m = (Matrix3Double) value[i];
            for (int j = 0; j < 9; j++)
                uniformBuffer.put((float) m.val[j]);
        }
        uniformBuffer.flip();
        Gdx.gl.glUniformMatrix3fv(handle, elementCount, false, uniformBuffer);
    }

    @Override
    protected void applyValue(Object[] value) {
        Matrix3Double[] dst = (Matrix3Double[]) this.value;
        for (int i = 0; i < Math.min(value.length, elementCount); i++)
            dst[i].set((Matrix3Double) value[i]);
    }

    public int elementCount() {
        return elementCount;
    }
}