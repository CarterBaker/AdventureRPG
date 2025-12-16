package com.AdventureRPG.core.shaderpipeline.uniforms.vectorarrays;

import com.AdventureRPG.core.shaderpipeline.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Vectors.Vector3Double;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class Vector3DoubleArrayUniform extends UniformAttribute<Vector3Double[]> {

    private ByteBuffer buffer;
    private FloatBuffer floatBuffer;

    public Vector3DoubleArrayUniform(int count) {
        super(new Vector3Double[count]);
        for (int i = 0; i < count; i++)
            value[i] = new Vector3Double();

        this.buffer = BufferUtils.newByteBuffer(count * 12); // as floats
        this.floatBuffer = buffer.asFloatBuffer();
    }

    @Override
    protected void push(int handle, Vector3Double[] value) {
        floatBuffer.clear();
        for (Vector3Double vec : value) {
            floatBuffer.put((float) vec.x);
            floatBuffer.put((float) vec.y);
            floatBuffer.put((float) vec.z);
        }
        floatBuffer.flip();

        Gdx.gl.glUniform3fv(handle, value.length, floatBuffer);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        buffer.clear();
        for (Vector3Double vec : value) {
            buffer.putFloat((float) vec.x);
            buffer.putFloat((float) vec.y);
            buffer.putFloat((float) vec.z);
        }
        buffer.flip();
        return buffer;
    }
}