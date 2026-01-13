package com.internal.bootstrap.shaderpipeline.uniforms.vectorarrays;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.internal.core.util.mathematics.vectors.Vector4;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public final class Vector4ArrayUniform extends UniformAttribute<float[]> {

    // Internal
    private final int elementCount;
    private final ByteBuffer buffer;
    private final FloatBuffer floatBuffer;

    public Vector4ArrayUniform(int elementCount) {

        // Internal
        super(new float[elementCount * 4]);
        this.elementCount = elementCount;
        this.buffer = BufferUtils.newByteBuffer(elementCount * 4 * 4);
        this.floatBuffer = buffer.asFloatBuffer();
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

    @Override
    public void set(float[] value) {
        System.arraycopy(value, 0, this.value, 0, Math.min(value.length, this.value.length));
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

    public int elementCount() {
        return elementCount;
    }
}
