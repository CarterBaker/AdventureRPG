package com.AdventureRPG.bootstrap.shaderpipeline.uniforms.vectors;

import com.AdventureRPG.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.mathematics.vectors.Vector2Int;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import java.nio.ByteBuffer;

public class Vector2IntUniform extends UniformAttribute<Vector2Int> {

    // Internal
    private final ByteBuffer buffer;

    public Vector2IntUniform() {

        // Internal
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

    @Override
    public void set(Vector2Int value) {
        this.value.set(value);
    }
}