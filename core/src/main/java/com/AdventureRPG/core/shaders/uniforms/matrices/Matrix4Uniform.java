package com.AdventureRPG.core.shaders.uniforms.matrices;

import com.AdventureRPG.core.shaders.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Mathematics.Matrices.Matrix4;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class Matrix4Uniform extends UniformAttribute<Object> {
    private ByteBuffer buffer;
    private FloatBuffer floatBuffer;
    private com.badlogic.gdx.math.Matrix4 gdxMatrix;

    public Matrix4Uniform() {
        super(new Matrix4());
        this.buffer = BufferUtils.newByteBuffer(64);
        this.floatBuffer = buffer.asFloatBuffer();
        this.gdxMatrix = new com.badlogic.gdx.math.Matrix4();
    }

    @Override
    protected void push(int handle, Object value) {
        if (value instanceof com.badlogic.gdx.math.Matrix4) {
            Gdx.gl.glUniformMatrix4fv(handle, 1, false, ((com.badlogic.gdx.math.Matrix4) value).val, 0);
        } else {
            Matrix4 m = (Matrix4) value;
            float[] val = gdxMatrix.val;
            val[0] = m.m00;
            val[1] = m.m10;
            val[2] = m.m20;
            val[3] = m.m30;
            val[4] = m.m01;
            val[5] = m.m11;
            val[6] = m.m21;
            val[7] = m.m31;
            val[8] = m.m02;
            val[9] = m.m12;
            val[10] = m.m22;
            val[11] = m.m32;
            val[12] = m.m03;
            val[13] = m.m13;
            val[14] = m.m23;
            val[15] = m.m33;
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
            Matrix4 m = (Matrix4) value;
            float[] val = gdxMatrix.val;
            val[0] = m.m00;
            val[1] = m.m10;
            val[2] = m.m20;
            val[3] = m.m30;
            val[4] = m.m01;
            val[5] = m.m11;
            val[6] = m.m21;
            val[7] = m.m31;
            val[8] = m.m02;
            val[9] = m.m12;
            val[10] = m.m22;
            val[11] = m.m32;
            val[12] = m.m03;
            val[13] = m.m13;
            val[14] = m.m23;
            val[15] = m.m33;
            floatBuffer.clear();
            floatBuffer.put(val, 0, 16);
            floatBuffer.flip();
        }
        return buffer;
    }
}