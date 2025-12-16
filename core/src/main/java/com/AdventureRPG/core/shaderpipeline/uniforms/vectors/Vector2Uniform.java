package com.AdventureRPG.core.shaderpipeline.uniforms.vectors;

import com.AdventureRPG.core.shaderpipeline.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Vectors.Vector2;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;

public class Vector2Uniform extends UniformAttribute<Vector2> {

    private ByteBuffer buffer;

    public Vector2Uniform() {
        super(new Vector2());
        this.buffer = BufferUtils.newByteBuffer(8); // 2 floats * 4 bytes
    }

    @Override
    protected void push(int handle, Vector2 value) {
        Gdx.gl.glUniform2f(handle, value.x, value.y);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        buffer.clear();
        buffer.putFloat(value.x);
        buffer.putFloat(value.y);
        buffer.flip();
        return buffer;
    }
}