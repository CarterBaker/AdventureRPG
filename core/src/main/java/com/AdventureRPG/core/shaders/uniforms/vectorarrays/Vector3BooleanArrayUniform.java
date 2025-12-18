package com.AdventureRPG.core.shaders.uniforms.vectorarrays;

import com.AdventureRPG.core.shaders.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Vectors.Vector3Boolean;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class Vector3BooleanArrayUniform extends UniformAttribute<Vector3Boolean[]> {

    private ByteBuffer buffer;
    private IntBuffer intBuffer;

    public Vector3BooleanArrayUniform(int count) {
        super(new Vector3Boolean[count]);
        for (int i = 0; i < count; i++)
            value[i] = new Vector3Boolean();

        this.buffer = BufferUtils.newByteBuffer(count * 12);
        this.intBuffer = buffer.asIntBuffer();
    }

    @Override
    protected void push(int handle, Vector3Boolean[] value) {
        intBuffer.clear();
        for (Vector3Boolean vec : value) {
            intBuffer.put(vec.x ? 1 : 0);
            intBuffer.put(vec.y ? 1 : 0);
            intBuffer.put(vec.z ? 1 : 0);
        }
        intBuffer.flip();

        Gdx.gl.glUniform3iv(handle, value.length, intBuffer);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        buffer.clear();
        for (Vector3Boolean vec : value) {
            buffer.putInt(vec.x ? 1 : 0);
            buffer.putInt(vec.y ? 1 : 0);
            buffer.putInt(vec.z ? 1 : 0);
        }
        buffer.flip();
        return buffer;
    }
}