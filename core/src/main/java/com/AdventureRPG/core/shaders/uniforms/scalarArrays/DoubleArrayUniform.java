package com.AdventureRPG.core.shaders.uniforms.scalarArrays;

import com.AdventureRPG.core.shaders.uniforms.UniformAttribute;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class DoubleArrayUniform extends UniformAttribute<Double[]> {

    private ByteBuffer buffer;
    private FloatBuffer floatBuffer;

    public DoubleArrayUniform(int count) {
        super(new Double[count]);
        for (int i = 0; i < count; i++)
            value[i] = 0.0d;

        this.buffer = BufferUtils.newByteBuffer(count * 4); // 4 bytes per float
        this.floatBuffer = buffer.asFloatBuffer();
    }

    @Override
    protected void push(int handle, Double[] value) {
        floatBuffer.clear();
        for (Double d : value) {
            floatBuffer.put(d.floatValue());
        }
        floatBuffer.flip();

        Gdx.gl.glUniform1fv(handle, value.length, floatBuffer);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        buffer.clear();
        for (Double d : value) {
            buffer.putFloat(d.floatValue()); // Convert to float for UBO
        }
        buffer.flip();
        return buffer;
    }
}