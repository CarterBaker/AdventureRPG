package com.AdventureRPG.core.shaders.uniforms.scalarArrays;

import com.AdventureRPG.core.shaders.uniforms.UniformAttribute;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class BooleanArrayUniform extends UniformAttribute<Boolean[]> {

    private ByteBuffer buffer;
    private IntBuffer intBuffer;

    public BooleanArrayUniform(int count) {
        super(new Boolean[count]);
        for (int i = 0; i < count; i++)
            value[i] = Boolean.FALSE;

        this.buffer = BufferUtils.newByteBuffer(count * 4);
        this.intBuffer = buffer.asIntBuffer();
    }

    @Override
    protected void push(int handle, Boolean[] value) {
        intBuffer.clear();
        for (Boolean b : value) {
            intBuffer.put(b ? 1 : 0);
        }
        intBuffer.flip();

        Gdx.gl.glUniform1iv(handle, value.length, intBuffer);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        buffer.clear();
        for (Boolean b : value) {
            buffer.putInt(b ? 1 : 0);
        }
        buffer.flip();
        return buffer;
    }
}