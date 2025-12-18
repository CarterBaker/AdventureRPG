package com.AdventureRPG.core.shaders.uniforms.scalarArrays;

import com.AdventureRPG.core.shaders.uniforms.UniformAttribute;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class IntegerArrayUniform extends UniformAttribute<Integer[]> {

    private ByteBuffer buffer;
    private IntBuffer intBuffer;

    public IntegerArrayUniform(int count) {
        super(new Integer[count]);
        for (int i = 0; i < count; i++)
            value[i] = 0;

        this.buffer = BufferUtils.newByteBuffer(count * 4);
        this.intBuffer = buffer.asIntBuffer();
    }

    @Override
    protected void push(int handle, Integer[] value) {
        intBuffer.clear();
        for (Integer i : value) {
            intBuffer.put(i);
        }
        intBuffer.flip();

        Gdx.gl.glUniform1iv(handle, value.length, intBuffer);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        buffer.clear();
        for (Integer i : value) {
            buffer.putInt(i);
        }
        buffer.flip();
        return buffer;
    }
}