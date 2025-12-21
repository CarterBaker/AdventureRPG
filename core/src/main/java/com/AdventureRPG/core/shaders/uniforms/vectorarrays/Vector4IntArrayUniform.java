package com.AdventureRPG.core.shaders.uniforms.vectorarrays;

import com.AdventureRPG.core.shaders.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Mathematics.Vectors.Vector4Int;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public final class Vector4IntArrayUniform extends UniformAttribute<int[]> {

    private static final int COMPONENTS = 4;

    private final int elementCount;

    private final ByteBuffer buffer;
    private final IntBuffer intBuffer;

    public Vector4IntArrayUniform(int elementCount) {
        super(new int[elementCount * COMPONENTS]);
        this.elementCount = elementCount;

        this.buffer = BufferUtils.newByteBuffer(elementCount * COMPONENTS * 4);
        this.intBuffer = buffer.asIntBuffer();
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

    public int elementCount() {
        return elementCount;
    }
}
