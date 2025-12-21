package com.AdventureRPG.core.shaders.uniforms.vectorarrays;

import com.AdventureRPG.core.shaders.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Mathematics.Vectors.Vector2Boolean;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public final class Vector2BooleanArrayUniform extends UniformAttribute<int[]> {

    private static final int COMPONENTS = 2;

    private final int elementCount;

    private final ByteBuffer buffer;
    private final IntBuffer intBuffer;

    public Vector2BooleanArrayUniform(int elementCount) {
        super(new int[elementCount * COMPONENTS]);
        this.elementCount = elementCount;

        this.buffer = BufferUtils.newByteBuffer(elementCount * COMPONENTS * 4);
        this.intBuffer = buffer.asIntBuffer();
    }

    public void set(Vector2Boolean[] vectors) {
        int idx = 0;
        for (Vector2Boolean v : vectors) {
            value[idx++] = v.x ? 1 : 0;
            value[idx++] = v.y ? 1 : 0;
        }
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

    public int elementCount() {
        return elementCount;
    }
}
