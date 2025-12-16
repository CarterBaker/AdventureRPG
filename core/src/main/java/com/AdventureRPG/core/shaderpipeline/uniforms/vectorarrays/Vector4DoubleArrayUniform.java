package com.AdventureRPG.core.shaderpipeline.uniforms.vectorarrays;

import com.AdventureRPG.core.shaderpipeline.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Vectors.Vector4Double;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class Vector4DoubleArrayUniform extends UniformAttribute<Vector4Double[]> {

    private ByteBuffer buffer;
    private FloatBuffer floatBuffer;

    public Vector4DoubleArrayUniform(int count) {
        super(new Vector4Double[count]);
        for (int i = 0; i < count; i++)
            value[i] = new Vector4Double();

        this.buffer = BufferUtils.newByteBuffer(count * 16); // as floats
        this.floatBuffer = buffer.asFloatBuffer();
    }

    @Override
    protected void push(int handle, Vector4Double[] value) {
        floatBuffer.clear();
        for (Vector4Double vec : value) {
            floatBuffer.put((float) vec.x);
            floatBuffer.put((float) vec.y);
            floatBuffer.put((float) vec.z);
            floatBuffer.put((float) vec.w);
        }
        floatBuffer.flip();

        Gdx.gl.glUniform4fv(handle, value.length, floatBuffer);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        buffer.clear();
        for (Vector4Double vec : value) {
            buffer.putFloat((float) vec.x);
            buffer.putFloat((float) vec.y);
            buffer.putFloat((float) vec.z);
            buffer.putFloat((float) vec.w);
        }
        buffer.flip();
        return buffer;
    }
}