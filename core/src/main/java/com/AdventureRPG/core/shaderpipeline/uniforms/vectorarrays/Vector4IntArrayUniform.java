package com.AdventureRPG.core.shaderpipeline.uniforms.vectorarrays;

import com.AdventureRPG.core.shaderpipeline.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Vectors.Vector4Int;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class Vector4IntArrayUniform extends UniformAttribute<Vector4Int[]> {

    private ByteBuffer buffer;
    private IntBuffer intBuffer;

    public Vector4IntArrayUniform(int count) {
        super(new Vector4Int[count]);
        for (int i = 0; i < count; i++)
            value[i] = new Vector4Int();

        this.buffer = BufferUtils.newByteBuffer(count * 16);
        this.intBuffer = buffer.asIntBuffer();
    }

    @Override
    protected void push(int handle, Vector4Int[] value) {
        intBuffer.clear();
        for (Vector4Int vec : value) {
            intBuffer.put(vec.x);
            intBuffer.put(vec.y);
            intBuffer.put(vec.z);
            intBuffer.put(vec.w);
        }
        intBuffer.flip();

        Gdx.gl.glUniform4iv(handle, value.length, intBuffer);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        buffer.clear();
        for (Vector4Int vec : value) {
            buffer.putInt(vec.x);
            buffer.putInt(vec.y);
            buffer.putInt(vec.z);
            buffer.putInt(vec.w);
        }
        buffer.flip();
        return buffer;
    }
}