package com.AdventureRPG.core.shaders.uniforms.vectorarrays;

import com.AdventureRPG.core.shaders.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Vectors.Vector2Int;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class Vector2IntArrayUniform extends UniformAttribute<Vector2Int[]> {

    private ByteBuffer buffer;
    private IntBuffer intBuffer;

    public Vector2IntArrayUniform(int count) {
        super(new Vector2Int[count]);
        for (int i = 0; i < count; i++)
            value[i] = new Vector2Int();

        this.buffer = BufferUtils.newByteBuffer(count * 8);
        this.intBuffer = buffer.asIntBuffer();
    }

    @Override
    protected void push(int handle, Vector2Int[] value) {
        intBuffer.clear();
        for (Vector2Int vec : value) {
            intBuffer.put(vec.x);
            intBuffer.put(vec.y);
        }
        intBuffer.flip();

        Gdx.gl.glUniform2iv(handle, value.length, intBuffer);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        buffer.clear();
        for (Vector2Int vec : value) {
            buffer.putInt(vec.x);
            buffer.putInt(vec.y);
        }
        buffer.flip();
        return buffer;
    }
}