package com.AdventureRPG.core.shaders.uniforms.vectorarrays;

import com.AdventureRPG.core.shaders.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Vectors.Vector4Boolean;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class Vector4BooleanArrayUniform extends UniformAttribute<Vector4Boolean[]> {

    private ByteBuffer buffer;
    private IntBuffer intBuffer;

    public Vector4BooleanArrayUniform(int count) {
        super(new Vector4Boolean[count]);
        for (int i = 0; i < count; i++)
            value[i] = new Vector4Boolean();

        this.buffer = BufferUtils.newByteBuffer(count * 16);
        this.intBuffer = buffer.asIntBuffer();
    }

    @Override
    protected void push(int handle, Vector4Boolean[] value) {
        intBuffer.clear();
        for (Vector4Boolean vec : value) {
            intBuffer.put(vec.x ? 1 : 0);
            intBuffer.put(vec.y ? 1 : 0);
            intBuffer.put(vec.z ? 1 : 0);
            intBuffer.put(vec.w ? 1 : 0);
        }
        intBuffer.flip();

        Gdx.gl.glUniform4iv(handle, value.length, intBuffer);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        buffer.clear();
        for (Vector4Boolean vec : value) {
            buffer.putInt(vec.x ? 1 : 0);
            buffer.putInt(vec.y ? 1 : 0);
            buffer.putInt(vec.z ? 1 : 0);
            buffer.putInt(vec.w ? 1 : 0);
        }
        buffer.flip();
        return buffer;
    }
}