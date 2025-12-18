package com.AdventureRPG.core.shaders.uniforms.vectors;

import com.AdventureRPG.core.shaders.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Vectors.Vector2Int;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;

public class Vector2IntUniform extends UniformAttribute<Vector2Int> {

    private ByteBuffer buffer;

    public Vector2IntUniform() {
        super(new Vector2Int());
        this.buffer = BufferUtils.newByteBuffer(8); // 2 ints * 4 bytes
    }

    @Override
    protected void push(int handle, Vector2Int value) {
        Gdx.gl.glUniform2i(handle, value.x, value.y);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        buffer.clear();
        buffer.putInt(value.x);
        buffer.putInt(value.y);
        buffer.flip();
        return buffer;
    }
}