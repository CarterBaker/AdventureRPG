package com.internal.bootstrap.shaderpipeline.uniforms.vectorarrays;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.internal.core.util.mathematics.vectors.Vector2Double;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public final class Vector2DoubleArrayUniform extends UniformAttribute<float[]> {

    // Internal
    private final int elementCount;
    private final ByteBuffer buffer;
    private final FloatBuffer floatBuffer;

    public Vector2DoubleArrayUniform(int elementCount) {

        // Internal
        super(new float[elementCount * 2]);
        this.elementCount = elementCount;
        this.buffer = BufferUtils.newByteBuffer(elementCount * 4 * 4);
        this.floatBuffer = buffer.asFloatBuffer();
    }

    @Override
    public UniformAttribute<?> createDefault() {
        return new Vector2DoubleArrayUniform(elementCount);
    }

    @Override
    protected void push(int handle, float[] data) {
        Gdx.gl.glUniform2fv(handle, elementCount, data, 0);
    }

    @Override
    public ByteBuffer getByteBuffer() {

        floatBuffer.clear();
        for (int i = 0; i < elementCount; i++) {
            floatBuffer.put(value[i * 2]);
            floatBuffer.put(value[i * 2 + 1]);
            floatBuffer.put(0f);
            floatBuffer.put(0f);
        }
        floatBuffer.flip();

        return buffer;
    }

    @Override
    public void set(float[] value) {
        System.arraycopy(value, 0, this.value, 0, Math.min(value.length, this.value.length));
        super.set(value);
    }

    public void set(Vector2Double[] vectors) {

        int idx = 0;

        for (Vector2Double v : vectors) {
            value[idx++] = (float) v.x;
            value[idx++] = (float) v.y;
        }

        super.set(value);
    }

    public int elementCount() {
        return elementCount;
    }
}