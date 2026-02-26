package com.internal.bootstrap.shaderpipeline.uniforms.vectorarrays;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.internal.core.util.mathematics.vectors.Vector4Boolean;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public final class Vector4BooleanArrayUniform extends UniformAttribute<int[]> {

    // Internal
    private final int elementCount;
    private final ByteBuffer buffer;
    private final IntBuffer intBuffer;

    public Vector4BooleanArrayUniform(int elementCount) {
        // Internal
        super(new int[elementCount * 4]);
        this.elementCount = elementCount;
        this.buffer = BufferUtils.newByteBuffer(elementCount * 16); // bvec4 = 16 bytes per element, no padding needed
                                                                    // (std140)
        this.intBuffer = buffer.asIntBuffer();
    }

    @Override
    public UniformAttribute<?> createDefault() {
        return new Vector4BooleanArrayUniform(elementCount);
    }

    @Override
    protected void push(int handle, int[] data) {
        Gdx.gl.glUniform4iv(handle, elementCount, data, 0);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        intBuffer.clear();
        intBuffer.put(value);
        intBuffer.flip();
        return buffer;
    }

    @Override
    protected void applyValue(int[] value) {
        System.arraycopy(value, 0, this.value, 0, Math.min(value.length, this.value.length));
    }

    @Override
    protected void applyObject(Object value) {
        if (value instanceof Vector4Boolean[] vectors) {
            for (int i = 0; i < vectors.length && i < elementCount; i++) {
                this.value[i * 4] = vectors[i].x ? 1 : 0;
                this.value[i * 4 + 1] = vectors[i].y ? 1 : 0;
                this.value[i * 4 + 2] = vectors[i].z ? 1 : 0;
                this.value[i * 4 + 3] = vectors[i].w ? 1 : 0;
            }
        } else {
            applyValue((int[]) value);
        }
    }

    public int elementCount() {
        return elementCount;
    }
}