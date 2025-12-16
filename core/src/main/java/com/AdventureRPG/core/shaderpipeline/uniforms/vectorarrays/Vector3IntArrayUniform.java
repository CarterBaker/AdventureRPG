package com.AdventureRPG.core.shaderpipeline.uniforms.vectorarrays;

import com.AdventureRPG.core.shaderpipeline.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Vectors.Vector3Int;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class Vector3IntArrayUniform extends UniformAttribute<Vector3Int[]> {

    private ByteBuffer buffer;
    private IntBuffer intBuffer;

    public Vector3IntArrayUniform(int count) {
        super(new Vector3Int[count]);
        for (int i = 0; i < count; i++)
            value[i] = new Vector3Int();

        this.buffer = BufferUtils.newByteBuffer(count * 12);
        this.intBuffer = buffer.asIntBuffer();
    }

    @Override
    protected void push(int handle, Vector3Int[] value) {
        intBuffer.clear();
        for (Vector3Int vec : value) {
            intBuffer.put(vec.x);
            intBuffer.put(vec.y);
            intBuffer.put(vec.z);
        }
        intBuffer.flip();

        Gdx.gl.glUniform3iv(handle, value.length, intBuffer);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        buffer.clear();
        for (Vector3Int vec : value) {
            buffer.putInt(vec.x);
            buffer.putInt(vec.y);
            buffer.putInt(vec.z);
        }
        buffer.flip();
        return buffer;
    }
}