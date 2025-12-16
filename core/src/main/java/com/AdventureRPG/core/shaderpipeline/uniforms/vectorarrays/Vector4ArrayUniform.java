package com.AdventureRPG.core.shaderpipeline.uniforms.vectorarrays;

import com.AdventureRPG.core.shaderpipeline.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Vectors.Vector4;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class Vector4ArrayUniform extends UniformAttribute<Vector4[]> {

    private ByteBuffer buffer;
    private FloatBuffer floatBuffer;

    public Vector4ArrayUniform(int count) {
        super(new Vector4[count]);
        for (int i = 0; i < count; i++)
            value[i] = new Vector4();

        this.buffer = BufferUtils.newByteBuffer(count * 16); // count * 4 floats * 4 bytes
        this.floatBuffer = buffer.asFloatBuffer();
    }

    @Override
    protected void push(int handle, Vector4[] value) {
        floatBuffer.clear();
        for (Vector4 vec : value) {
            floatBuffer.put(vec.x);
            floatBuffer.put(vec.y);
            floatBuffer.put(vec.z);
            floatBuffer.put(vec.w);
        }
        floatBuffer.flip();

        Gdx.gl.glUniform4fv(handle, value.length, floatBuffer);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        buffer.clear();
        for (Vector4 vec : value) {
            buffer.putFloat(vec.x);
            buffer.putFloat(vec.y);
            buffer.putFloat(vec.z);
            buffer.putFloat(vec.w);
        }
        buffer.flip();
        return buffer;
    }
}