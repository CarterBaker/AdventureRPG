package com.AdventureRPG.core.shaderpipeline.uniforms.vectorarrays;

import com.AdventureRPG.core.shaderpipeline.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Vectors.Vector2;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class Vector2ArrayUniform extends UniformAttribute<Vector2[]> {

    private ByteBuffer buffer;
    private FloatBuffer floatBuffer;

    public Vector2ArrayUniform(int count) {
        super(new Vector2[count]);
        for (int i = 0; i < count; i++)
            value[i] = new Vector2();

        this.buffer = BufferUtils.newByteBuffer(count * 8); // count * 2 floats * 4 bytes
        this.floatBuffer = buffer.asFloatBuffer();
    }

    @Override
    protected void push(int handle, Vector2[] value) {
        floatBuffer.clear();
        for (Vector2 vec : value) {
            floatBuffer.put(vec.x);
            floatBuffer.put(vec.y);
        }
        floatBuffer.flip();

        Gdx.gl.glUniform2fv(handle, value.length, floatBuffer);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        buffer.clear();
        for (Vector2 vec : value) {
            buffer.putFloat(vec.x);
            buffer.putFloat(vec.y);
        }
        buffer.flip();
        return buffer;
    }
}