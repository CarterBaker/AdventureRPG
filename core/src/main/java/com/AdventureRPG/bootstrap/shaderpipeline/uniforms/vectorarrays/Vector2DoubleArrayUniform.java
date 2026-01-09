package com.AdventureRPG.bootstrap.shaderpipeline.uniforms.vectorarrays;

import com.AdventureRPG.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.mathematics.vectors.Vector2Double;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

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
        this.buffer = BufferUtils.newByteBuffer(elementCount * 2 * 4);
        this.floatBuffer = buffer.asFloatBuffer();
    }

    @Override
    protected void push(int handle, float[] data) {
        Gdx.gl.glUniform2fv(handle, elementCount, data, 0);
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

    public void set(Vector2Double[] vectors) {

        int idx = 0;

        for (Vector2Double v : vectors) {
            value[idx++] = (float) v.x;
            value[idx++] = (float) v.y;
        }
    }

    public int elementCount() {
        return elementCount;
    }
}