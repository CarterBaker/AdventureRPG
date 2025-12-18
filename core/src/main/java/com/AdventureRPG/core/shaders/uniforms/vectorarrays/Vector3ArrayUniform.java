package com.AdventureRPG.core.shaders.uniforms.vectorarrays;

import com.AdventureRPG.core.shaders.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Vectors.Vector3;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class Vector3ArrayUniform extends UniformAttribute<Vector3[]> {

    private ByteBuffer buffer;
    private FloatBuffer floatBuffer;

    public Vector3ArrayUniform(int count) {
        super(new Vector3[count]);
        for (int i = 0; i < count; i++)
            value[i] = new Vector3();

        this.buffer = BufferUtils.newByteBuffer(count * 12); // count * 3 floats * 4 bytes
        this.floatBuffer = buffer.asFloatBuffer();
    }

    @Override
    protected void push(int handle, Vector3[] value) {
        floatBuffer.clear();
        for (Vector3 vec : value) {
            floatBuffer.put(vec.x);
            floatBuffer.put(vec.y);
            floatBuffer.put(vec.z);
        }
        floatBuffer.flip();

        Gdx.gl.glUniform3fv(handle, value.length, floatBuffer);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        buffer.clear();
        for (Vector3 vec : value) {
            buffer.putFloat(vec.x);
            buffer.putFloat(vec.y);
            buffer.putFloat(vec.z);
        }
        buffer.flip();
        return buffer;
    }
}