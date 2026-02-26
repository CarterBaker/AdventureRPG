package com.internal.bootstrap.shaderpipeline.uniforms.matrices;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.internal.core.util.mathematics.matrices.Matrix4;

import java.nio.ByteBuffer;

public final class Matrix4Uniform extends UniformAttribute<Object> {

    // Internal
    private final ByteBuffer uboBuffer;

    public Matrix4Uniform() {
        // Internal
        super(new Matrix4());
        this.uboBuffer = BufferUtils.newByteBuffer(64); // (std140): 4 columns * (vec4) 4 floats * 4 bytes = 64 bytes
    }

    @Override
    public UniformAttribute<?> createDefault() {
        return new Matrix4Uniform();
    }

    @Override
    protected void push(int handle, Object value) {
        // Push libGDX matrix
        if (value instanceof com.badlogic.gdx.math.Matrix4 gdxMatrix)
            Gdx.gl.glUniformMatrix4fv(handle, 1, false, gdxMatrix.val, 0);
        // Push internal matrix
        else if (value instanceof Matrix4 internalMatrix)
            Gdx.gl.glUniformMatrix4fv(handle, 1, false, internalMatrix.val, 0);
        else
            throw new IllegalArgumentException(
                    "push(int, Matrix4): Expected Matrix4 or com.badlogic.gdx.math.Matrix4, got " + value.getClass());
    }

    @Override
    public ByteBuffer getByteBuffer() {
        Matrix4 matrix = (Matrix4) value;
        uboBuffer.clear();
        // Column 0
        uboBuffer.putFloat(matrix.val[0]); // m00
        uboBuffer.putFloat(matrix.val[1]); // m10
        uboBuffer.putFloat(matrix.val[2]); // m20
        uboBuffer.putFloat(matrix.val[3]); // m30
        // Column 1
        uboBuffer.putFloat(matrix.val[4]); // m01
        uboBuffer.putFloat(matrix.val[5]); // m11
        uboBuffer.putFloat(matrix.val[6]); // m21
        uboBuffer.putFloat(matrix.val[7]); // m31
        // Column 2
        uboBuffer.putFloat(matrix.val[8]); // m02
        uboBuffer.putFloat(matrix.val[9]); // m12
        uboBuffer.putFloat(matrix.val[10]); // m22
        uboBuffer.putFloat(matrix.val[11]); // m32
        // Column 3
        uboBuffer.putFloat(matrix.val[12]); // m03
        uboBuffer.putFloat(matrix.val[13]); // m13
        uboBuffer.putFloat(matrix.val[14]); // m23
        uboBuffer.putFloat(matrix.val[15]); // m33
        uboBuffer.flip();
        return uboBuffer;
    }

    @Override
    protected void applyValue(Object value) {
        Matrix4 target = (Matrix4) this.value;
        if (value instanceof com.badlogic.gdx.math.Matrix4 gdxMatrix)
            target.fromGDX(gdxMatrix);
        else if (value instanceof Matrix4 internalMatrix)
            target.set(internalMatrix);
        else
            throw new IllegalArgumentException(
                    "applyValue(Matrix4): Expected Matrix4 or com.badlogic.gdx.math.Matrix4, got " + value.getClass());
    }

    @Override
    protected void applyObject(Object value) {
        applyValue(value);
    }
}