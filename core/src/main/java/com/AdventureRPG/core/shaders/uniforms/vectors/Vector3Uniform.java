package com.AdventureRPG.core.shaders.uniforms.vectors;

import com.AdventureRPG.core.shaders.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Mathematics.Vectors.Vector3;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import java.nio.ByteBuffer;

public class Vector3Uniform extends UniformAttribute<Object> {
    private ByteBuffer buffer;

    public Vector3Uniform() {
        super(new Vector3());
        this.buffer = BufferUtils.newByteBuffer(12);
    }

    @Override
    protected void push(int handle, Object value) {
        if (value instanceof com.badlogic.gdx.math.Vector3) {
            com.badlogic.gdx.math.Vector3 v = (com.badlogic.gdx.math.Vector3) value;
            Gdx.gl.glUniform3f(handle, v.x, v.y, v.z);
        } else {
            Vector3 v = (Vector3) value;
            Gdx.gl.glUniform3f(handle, v.x, v.y, v.z);
        }
    }

    @Override
    public ByteBuffer getByteBuffer() {
        buffer.clear();
        if (value instanceof com.badlogic.gdx.math.Vector3) {
            com.badlogic.gdx.math.Vector3 v = (com.badlogic.gdx.math.Vector3) value;
            buffer.putFloat(v.x);
            buffer.putFloat(v.y);
            buffer.putFloat(v.z);
        } else {
            Vector3 v = (Vector3) value;
            buffer.putFloat(v.x);
            buffer.putFloat(v.y);
            buffer.putFloat(v.z);
        }
        buffer.flip();
        return buffer;
    }
}