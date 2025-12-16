package com.AdventureRPG.core.shaderpipeline.uniforms.scalarArrays;

import com.AdventureRPG.core.shaderpipeline.uniforms.UniformAttribute;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class FloatArrayUniform extends UniformAttribute<Float[]> {

    private ByteBuffer buffer;
    private FloatBuffer floatBuffer;

    public FloatArrayUniform(int count) {
        super(new Float[count]);
        for (int i = 0; i < count; i++)
            value[i] = 0.0f;

        this.buffer = BufferUtils.newByteBuffer(count * 4);
        this.floatBuffer = buffer.asFloatBuffer();
    }

    @Override
    protected void push(int handle, Float[] value) {
        floatBuffer.clear();
        for (Float f : value) {
            floatBuffer.put(f);
        }
        floatBuffer.flip();

        Gdx.gl.glUniform1fv(handle, value.length, floatBuffer);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        buffer.clear();
        for (Float f : value) {
            buffer.putFloat(f);
        }
        buffer.flip();
        return buffer;
    }
}