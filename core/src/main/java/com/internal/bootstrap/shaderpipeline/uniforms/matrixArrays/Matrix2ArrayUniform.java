package com.internal.bootstrap.shaderpipeline.uniforms.matrixArrays;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformType;
import com.internal.core.util.mathematics.matrices.Matrix2;

import java.nio.FloatBuffer;

public final class Matrix2ArrayUniform extends UniformAttribute<Object[]> {

    private final int elementCount;
    private final FloatBuffer uniformBuffer;

    public Matrix2ArrayUniform(int elementCount) {
        super(UniformType.MATRIX2, elementCount, new Matrix2[elementCount]);
        this.elementCount = elementCount;
        this.uniformBuffer = BufferUtils.newFloatBuffer(elementCount * 4);
        for (int i = 0; i < elementCount; i++)
            ((Matrix2[]) value)[i] = new Matrix2();
    }

    @Override
    public UniformAttribute<?> createDefault() {
        return new Matrix2ArrayUniform(elementCount);
    }

    @Override
    protected void push(int handle, Object[] value) {
        uniformBuffer.clear();
        for (int i = 0; i < elementCount; i++) {
            Matrix2 m = (Matrix2) value[i];
            uniformBuffer.put(m.val[0]);
            uniformBuffer.put(m.val[1]);
            uniformBuffer.put(m.val[2]);
            uniformBuffer.put(m.val[3]);
        }
        uniformBuffer.flip();
        Gdx.gl.glUniformMatrix2fv(handle, elementCount, false, uniformBuffer);
    }

    @Override
    protected void applyValue(Object[] value) {
        Matrix2[] dst = (Matrix2[]) this.value;
        for (int i = 0; i < Math.min(value.length, elementCount); i++)
            dst[i].set((Matrix2) value[i]);
    }

    public int elementCount() {
        return elementCount;
    }
}