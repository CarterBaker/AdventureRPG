package com.AdventureRPG.core.shaderpipeline.uniforms.vectorarrays;

import com.AdventureRPG.core.shaderpipeline.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Mathematics.Vectors.Vector2Boolean;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

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
    }

    public void set(Vector2Boolean[] vectors) {

        int idx = 0;

        for (Vector2Boolean v : vectors) {
            value[idx++] = v.x ? 1 : 0;
            value[idx++] = v.y ? 1 : 0;
        }
    }

    public int elementCount() {
        return elementCount;
    }
}
