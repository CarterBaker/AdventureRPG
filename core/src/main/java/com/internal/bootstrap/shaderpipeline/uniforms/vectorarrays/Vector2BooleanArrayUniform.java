package com.internal.bootstrap.shaderpipeline.uniforms.vectorarrays;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.internal.core.util.mathematics.vectors.Vector2Boolean;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public final class Vector2BooleanArrayUniform extends UniformAttribute<int[]> {

    // Internal
    private final int elementCount;
    private final ByteBuffer buffer;
    private final IntBuffer intBuffer;

    public Vector2BooleanArrayUniform(int elementCount) {

        // Internal
        super(new int[elementCount * 2]);
        this.elementCount = elementCount;
        this.buffer = BufferUtils.newByteBuffer(elementCount * 2 * 4);
        this.intBuffer = buffer.asIntBuffer();
    }

    @Override
    public UniformAttribute<?> createDefault() {
        return new Vector2BooleanArrayUniform(elementCount);
    }

    @Override
    protected void push(int handle, int[] data) {
        Gdx.gl.glUniform2iv(handle, elementCount, data, 0);
    }

    @Override
    public ByteBuffer getByteBuffer() {

        intBuffer.clear();
        intBuffer.put(value);
        intBuffer.flip();

        return buffer;
    }

    @Override
    public void set(int[] value) {
        System.arraycopy(value, 0, this.value, 0, Math.min(value.length, this.value.length));
        super.set(value);
    }

    public void set(Vector2Boolean[] vectors) {

        int idx = 0;

        for (Vector2Boolean v : vectors) {
            value[idx++] = v.x ? 1 : 0;
            value[idx++] = v.y ? 1 : 0;
        }

        super.set(value);
    }

    public int elementCount() {
        return elementCount;
    }
}
