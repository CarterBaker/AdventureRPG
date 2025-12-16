package com.AdventureRPG.core.shaderpipeline.uniforms.vectorarrays;

import com.AdventureRPG.core.shaderpipeline.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Vectors.Vector2Boolean;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class Vector2BooleanArrayUniform extends UniformAttribute<Vector2Boolean[]> {

    private ByteBuffer buffer;
    private IntBuffer intBuffer;

    public Vector2BooleanArrayUniform(int count) {
        super(new Vector2Boolean[count]);
        for (int i = 0; i < count; i++)
            value[i] = new Vector2Boolean();

        this.buffer = BufferUtils.newByteBuffer(count * 8);
        this.intBuffer = buffer.asIntBuffer();
    }

    @Override
    protected void push(int handle, Vector2Boolean[] value) {
        intBuffer.clear();
        for (Vector2Boolean vec : value) {
            intBuffer.put(vec.x ? 1 : 0);
            intBuffer.put(vec.y ? 1 : 0);
        }
        intBuffer.flip();

        Gdx.gl.glUniform2iv(handle, value.length, intBuffer);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        buffer.clear();
        for (Vector2Boolean vec : value) {
            buffer.putInt(vec.x ? 1 : 0);
            buffer.putInt(vec.y ? 1 : 0);
        }
        buffer.flip();
        return buffer;
    }
}