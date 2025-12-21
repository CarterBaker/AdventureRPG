package com.AdventureRPG.core.shaders.uniforms.matrices;

import com.AdventureRPG.core.shaders.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Mathematics.Matrices.Matrix4Double;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class Matrix4DoubleUniform extends UniformAttribute<Object> {
    private ByteBuffer buffer;
    private FloatBuffer floatBuffer;
    private com.badlogic.gdx.math.Matrix4 gdxMatrix;

    public Matrix4DoubleUniform() {
        super(new Matrix4Double());
        this.buffer = BufferUtils.newByteBuffer(64);
        this.floatBuffer = buffer.asFloatBuffer();
        this.gdxMatrix = new com.badlogic.gdx.math.Matrix4();
    }

    @Override
    protected void push(int handle, Object value) {
        if (value instanceof com.badlogic.gdx.math.Matrix4) {
            Gdx.gl.glUniformMatrix4fv(handle, 1, false, ((com.badlogic.gdx.math.Matrix4) value).val, 0);
        } else {
            Matrix4Double m = (Matrix4Double) value;
            float[] val = gdxMatrix.val;
            val[0] = (float) m.m00;
            val[1] = (float) m.m10;
            val[2] = (float) m.m20;
            val[3] = (float) m.m30;
            val[4] = (float) m.m01;
            val[5] = (float) m.m11;
            val[6] = (float) m.m21;
            val[7] = (float) m.m31;
            val[8] = (float) m.m02;
            val[9] = (float) m.m12;
            val[10] = (float) m.m22;
            val[11] = (float) m.m32;
            val[12] = (float) m.m03;
            val[13] = (float) m.m13;
            val[14] = (float) m.m23;
            val[15] = (float) m.m33;
            Gdx.gl.glUniformMatrix4fv(handle, 1, false, val, 0);
        }
    }

    @Override
    public ByteBuffer getByteBuffer() {
        buffer.clear();
        if (value instanceof com.badlogic.gdx.math.Matrix4) {
            floatBuffer.clear();
            floatBuffer.put(((com.badlogic.gdx.math.Matrix4) value).val, 0, 16);
            floatBuffer.flip();
        } else {
            Matrix4Double m = (Matrix4Double) value;
            float[] val = gdxMatrix.val;
            val[0] = (float) m.m00;
            val[1] = (float) m.m10;
            val[2] = (float) m.m20;
            val[3] = (float) m.m30;
            val[4] = (float) m.m01;
            val[5] = (float) m.m11;
            val[6] = (float) m.m21;
            val[7] = (float) m.m31;
            val[8] = (float) m.m02;
            val[9] = (float) m.m12;
            val[10] = (float) m.m22;
            val[11] = (float) m.m32;
            val[12] = (float) m.m03;
            val[13] = (float) m.m13;
            val[14] = (float) m.m23;
            val[15] = (float) m.m33;
            floatBuffer.clear();
            floatBuffer.put(val, 0, 16);
            floatBuffer.flip();
        }
        return buffer;
    }
}