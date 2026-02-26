package com.internal.bootstrap.shaderpipeline.uniforms.matrixArrays;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.internal.core.util.mathematics.matrices.Matrix3Double;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public final class Matrix3DoubleArrayUniform extends UniformAttribute<Matrix3Double[]> {

    // Internal
    private final int elementCount;
    private final ByteBuffer uboBuffer;
    private final FloatBuffer uniformBuffer;

    public Matrix3DoubleArrayUniform(int elementCount) {
        // Internal
        super(new Matrix3Double[elementCount]);
        this.elementCount = elementCount;
        this.uboBuffer = BufferUtils.newByteBuffer(elementCount * 48); // (std140): 3 columns * (vec4 padded) 4 floats *
                                                                       // 4 bytes = 48 bytes per element (doubles
                                                                       // downcast to float for GLSL ES)
        this.uniformBuffer = uboBuffer.asFloatBuffer();
        for (int i = 0; i < elementCount; i++)
            value[i] = new Matrix3Double();
    }

    @Override
    public UniformAttribute<?> createDefault() {
        return new Matrix3DoubleArrayUniform(elementCount);
    }

    @Override
    protected void push(int handle, Matrix3Double[] matrices) {
        uniformBuffer.clear();
        for (int i = 0; i < elementCount; i++) {
            Matrix3Double m = matrices[i];
            // Column 0
            uniformBuffer.put((float) m.val[0]); // m00
            uniformBuffer.put((float) m.val[3]); // m10
            uniformBuffer.put((float) m.val[6]); // m20
            // Column 1
            uniformBuffer.put((float) m.val[1]); // m01
            uniformBuffer.put((float) m.val[4]); // m11
            uniformBuffer.put((float) m.val[7]); // m21
            // Column 2
            uniformBuffer.put((float) m.val[2]); // m02
            uniformBuffer.put((float) m.val[5]); // m12
            uniformBuffer.put((float) m.val[8]); // m22
        }
        uniformBuffer.flip();
        Gdx.gl.glUniformMatrix3fv(handle, elementCount, false, uniformBuffer);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        uboBuffer.clear();
        for (int i = 0; i < elementCount; i++) {
            Matrix3Double m = value[i];
            // Column 0
            uboBuffer.putFloat((float) m.val[0]); // m00
            uboBuffer.putFloat((float) m.val[3]); // m10
            uboBuffer.putFloat((float) m.val[6]); // m20
            uboBuffer.putFloat(0f); // padding
            // Column 1
            uboBuffer.putFloat((float) m.val[1]); // m01
            uboBuffer.putFloat((float) m.val[4]); // m11
            uboBuffer.putFloat((float) m.val[7]); // m21
            uboBuffer.putFloat(0f); // padding
            // Column 2
            uboBuffer.putFloat((float) m.val[2]); // m02
            uboBuffer.putFloat((float) m.val[5]); // m12
            uboBuffer.putFloat((float) m.val[8]); // m22
            uboBuffer.putFloat(0f); // padding
        }
        uboBuffer.flip();
        return uboBuffer;
    }

    @Override
    protected void applyValue(Matrix3Double[] matrices) {
        for (int i = 0; i < elementCount; i++)
            this.value[i].set(matrices[i]);
    }

    public int elementCount() {
        return elementCount;
    }
}