package com.internal.bootstrap.shaderpipeline.uniforms.vectorarrays;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.internal.core.util.mathematics.vectors.Vector4Int;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public final class Vector4IntArrayUniform extends UniformAttribute<int[]> {

    // Internal
    private final int elementCount;
    private final ByteBuffer buffer;
    private final IntBuffer intBuffer;

    public Vector4IntArrayUniform(int elementCount) {

        // Internal
        super(new int[elementCount * 4]);
        this.elementCount = elementCount;
        this.buffer = BufferUtils.newByteBuffer(elementCount * 4 * 4);
        this.intBuffer = buffer.asIntBuffer();
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
    public void set(int[] value) {
        System.arraycopy(value, 0, this.value, 0, Math.min(value.length, this.value.length));
    }

    public void set(Vector4Int[] vectors) {

        int idx = 0;

        for (Vector4Int v : vectors) {
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
