package com.AdventureRPG.core.shaders.uniforms.matrices;

import com.AdventureRPG.core.shaders.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Mathematics.Matrices.Matrix3Double;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class Matrix3DoubleUniform extends UniformAttribute<Object> {
    private ByteBuffer buffer;
    private FloatBuffer floatBuffer;
    private com.badlogic.gdx.math.Matrix3 gdxMatrix;

    public Matrix3DoubleUniform() {
        super(new Matrix3Double());
        this.buffer = BufferUtils.newByteBuffer(36);
        this.floatBuffer = buffer.asFloatBuffer();
        this.gdxMatrix = new com.badlogic.gdx.math.Matrix3();
    }

    @Override
    protected void push(int handle, Object value) {
        if (value instanceof com.badlogic.gdx.math.Matrix3) {
            Gdx.gl.glUniformMatrix3fv(handle, 1, false, ((com.badlogic.gdx.math.Matrix3) value).val, 0);
        } else {
            Matrix3Double m = (Matrix3Double) value;
            float[] val = gdxMatrix.val;
            val[0] = (float) m.m00;
            val[1] = (float) m.m10;
            val[2] = (float) m.m20;
            val[3] = (float) m.m01;
            val[4] = (float) m.m11;
            val[5] = (float) m.m21;
            val[6] = (float) m.m02;
            val[7] = (float) m.m12;
            val[8] = (float) m.m22;
            Gdx.gl.glUniformMatrix3fv(handle, 1, false, val, 0);
        }
    }

    @Override
    public ByteBuffer getByteBuffer() {
        buffer.clear();
        if (value instanceof com.badlogic.gdx.math.Matrix3) {
            floatBuffer.clear();
            floatBuffer.put(((com.badlogic.gdx.math.Matrix3) value).val, 0, 9);
            floatBuffer.flip();
        } else {
            Matrix3Double m = (Matrix3Double) value;
            float[] val = gdxMatrix.val;
            val[0] = (float) m.m00;
            val[1] = (float) m.m10;
            val[2] = (float) m.m20;
            val[3] = (float) m.m01;
            val[4] = (float) m.m11;
            val[5] = (float) m.m21;
            val[6] = (float) m.m02;
            val[7] = (float) m.m12;
            val[8] = (float) m.m22;
            floatBuffer.clear();
            floatBuffer.put(val, 0, 9);
            floatBuffer.flip();
        }
        return buffer;
    }
}