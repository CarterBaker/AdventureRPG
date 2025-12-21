package com.AdventureRPG.core.shaders.uniforms.vectors;

import com.AdventureRPG.core.shaders.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Mathematics.Vectors.Vector2;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import java.nio.ByteBuffer;

public class Vector2Uniform extends UniformAttribute<Object> {
    private ByteBuffer buffer;

    public Vector2Uniform() {
        super(new Vector2());
        this.buffer = BufferUtils.newByteBuffer(8);
    }

    @Override
    protected void push(int handle, Object value) {
        if (value instanceof com.badlogic.gdx.math.Vector2) {
            com.badlogic.gdx.math.Vector2 v = (com.badlogic.gdx.math.Vector2) value;
            Gdx.gl.glUniform2f(handle, v.x, v.y);
        } else {
            Vector2 v = (Vector2) value;
            Gdx.gl.glUniform2f(handle, v.x, v.y);
        }
    }

    @Override
    public ByteBuffer getByteBuffer() {
        buffer.clear();
        if (value instanceof com.badlogic.gdx.math.Vector2) {
            com.badlogic.gdx.math.Vector2 v = (com.badlogic.gdx.math.Vector2) value;
            buffer.putFloat(v.x);
            buffer.putFloat(v.y);
        } else {
            Vector2 v = (Vector2) value;
            buffer.putFloat(v.x);
            buffer.putFloat(v.y);
        }
        buffer.flip();
        return buffer;
    }
}