package com.AdventureRPG.core.shaders.uniforms.vectorarrays;

import com.AdventureRPG.core.shaders.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Mathematics.Vectors.Vector4;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public final class Vector4ArrayUniform extends UniformAttribute<float[]> {

    private static final int COMPONENTS = 4;

    private final int elementCount;

    private final ByteBuffer buffer;
    private final FloatBuffer floatBuffer;

    public Vector4ArrayUniform(int elementCount) {
        super(new float[elementCount * COMPONENTS]);
        this.elementCount = elementCount;

        this.buffer = BufferUtils.newByteBuffer(elementCount * COMPONENTS * 4);
        this.floatBuffer = buffer.asFloatBuffer();
    }

    public void set(Vector4[] vectors) {
        int idx = 0;
        for (Vector4 v : vectors) {
            value[idx++] = v.x;
            value[idx++] = v.y;
            value[idx++] = v.z;
            value[idx++] = v.w;
        }
    }

    public void set(com.badlogic.gdx.math.Vector4[] vectors) {
        int idx = 0;
        for (com.badlogic.gdx.math.Vector4 v : vectors) {
            value[idx++] = v.x;
            value[idx++] = v.y;
            value[idx++] = v.z;
            value[idx++] = v.w;
        }
    }

    @Override
    protected void push(int handle, float[] data) {
        Gdx.gl.glUniform4fv(handle, elementCount, data, 0);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        floatBuffer.clear();
        floatBuffer.put(value);
        floatBuffer.flip();
        return buffer;
    }

    public int elementCount() {
        return elementCount;
    }
}
