package com.AdventureRPG.core.shaderpipeline.uniforms.vectorarrays;

import com.AdventureRPG.core.shaderpipeline.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Vectors.Vector2Double;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class Vector2DoubleArrayUniform extends UniformAttribute<Vector2Double[]> {

    private ByteBuffer buffer;
    private FloatBuffer floatBuffer;

    public Vector2DoubleArrayUniform(int count) {
        super(new Vector2Double[count]);
        for (int i = 0; i < count; i++)
            value[i] = new Vector2Double();

        this.buffer = BufferUtils.newByteBuffer(count * 8); // as floats
        this.floatBuffer = buffer.asFloatBuffer();
    }

    @Override
    protected void push(int handle, Vector2Double[] value) {
        floatBuffer.clear();
        for (Vector2Double vec : value) {
            floatBuffer.put((float) vec.x);
            floatBuffer.put((float) vec.y);
        }
        floatBuffer.flip();

        Gdx.gl.glUniform2fv(handle, value.length, floatBuffer);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        buffer.clear();
        for (Vector2Double vec : value) {
            buffer.putFloat((float) vec.x);
            buffer.putFloat((float) vec.y);
        }
        buffer.flip();
        return buffer;
    }
}