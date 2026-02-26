package com.internal.bootstrap.shaderpipeline.uniforms.vectorarrays;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.internal.core.util.mathematics.vectors.Vector2Int;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public final class Vector2IntArrayUniform extends UniformAttribute<int[]> {

    // Internal
    private final int elementCount;
    private final ByteBuffer buffer;
    private final IntBuffer intBuffer;

    public Vector2IntArrayUniform(int elementCount) {
        // Internal
        super(new int[elementCount * 2]);
        this.elementCount = elementCount;
        this.buffer = BufferUtils.newByteBuffer(elementCount * 16); // ivec2 padded to 16 bytes per element (std140)
        this.intBuffer = buffer.asIntBuffer();
    }

    @Override
    public UniformAttribute<?> createDefault() {
        return new Vector2IntArrayUniform(elementCount);
    }

    @Override
    protected void push(int handle, int[] data) {
        Gdx.gl.glUniform2iv(handle, elementCount, data, 0);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        intBuffer.clear();
        for (int i = 0; i < elementCount; i++) {
            intBuffer.put(value[i * 2]); // x
            intBuffer.put(value[i * 2 + 1]); // y
            intBuffer.put(0); // padding
            intBuffer.put(0); // padding
        }
        intBuffer.flip();
        return buffer;
    }

    @Override
    protected void applyValue(int[] value) {
        System.arraycopy(value, 0, this.value, 0, Math.min(value.length, this.value.length));
    }

    @Override
    protected void applyObject(Object value) {
        if (value instanceof Vector2Int[] vectors) {
            for (int i = 0; i < vectors.length && i < elementCount; i++) {
                this.value[i * 2] = vectors[i].x;
                this.value[i * 2 + 1] = vectors[i].y;
            }
        } else {
            applyValue((int[]) value);
        }
    }

    public int elementCount() {
        return elementCount;
    }
}