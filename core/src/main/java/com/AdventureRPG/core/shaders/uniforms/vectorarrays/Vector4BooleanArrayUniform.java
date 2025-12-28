package com.AdventureRPG.core.shaders.uniforms.vectorarrays;

import com.AdventureRPG.core.shaders.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Mathematics.Vectors.Vector4Boolean;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

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

    public void set(Vector4Boolean[] vectors) {

        int idx = 0;

        for (Vector4Boolean v : vectors) {
            value[idx++] = v.x ? 1 : 0;
            value[idx++] = v.y ? 1 : 0;
            value[idx++] = v.z ? 1 : 0;
            value[idx++] = v.w ? 1 : 0;
        }
    }

    public int elementCount() {
        return elementCount;
    }
}
