package com.internal.bootstrap.shaderpipeline.uniforms.matrixArrays;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformType;
import com.internal.core.util.mathematics.matrices.Matrix3;

import java.nio.FloatBuffer;

public final class Matrix3ArrayUniform extends UniformAttribute<Object[]> {

    private final int elementCount;
    private final FloatBuffer uniformBuffer;

    public Matrix3ArrayUniform(int elementCount) {
        super(UniformType.MATRIX3, elementCount, new Matrix3[elementCount]);
        this.elementCount = elementCount;
        this.uniformBuffer = BufferUtils.newFloatBuffer(elementCount * 9);
        for (int i = 0; i < elementCount; i++)
            ((Matrix3[]) value)[i] = new Matrix3();
    }

    @Override
    public UniformAttribute<?> createDefault() {
        return new Matrix3ArrayUniform(elementCount);
    }

    @Override
    protected void push(int handle, Object[] value) {
        uniformBuffer.clear();
        for (int i = 0; i < elementCount; i++)
            uniformBuffer.put(((Matrix3) value[i]).val, 0, 9);
        uniformBuffer.flip();
        Gdx.gl.glUniformMatrix3fv(handle, elementCount, false, uniformBuffer);
    }

    @Override
    protected void applyValue(Object[] value) {
        Matrix3[] dst = (Matrix3[]) this.value;
        for (int i = 0; i < Math.min(value.length, elementCount); i++)
            dst[i].set((Matrix3) value[i]);
    }

    @Override
    protected void applyObject(Object value) {
        if (value instanceof Matrix3[] m)
            applyValue(m);
        else if (value instanceof com.badlogic.gdx.math.Matrix3[] gdx) {
            Matrix3[] dst = (Matrix3[]) this.value;
            for (int i = 0; i < Math.min(gdx.length, elementCount); i++)
                dst[i].fromGDX(gdx[i]);
        } else
            throw new IllegalArgumentException("applyObject(Matrix3Array): got " + value.getClass());
    }

    public int elementCount() {
        return elementCount;
    }
}