package com.AdventureRPG.core.shaderpipeline.uniforms.vectors;

import com.AdventureRPG.core.shaderpipeline.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Mathematics.Vectors.Vector4Int;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import java.nio.ByteBuffer;

public class Vector4IntUniform extends UniformAttribute<Vector4Int> {

    // Internal
    private final ByteBuffer buffer;

    public Vector4IntUniform() {

        // Internal
        super(new Vector4Int());
        this.buffer = BufferUtils.newByteBuffer(16); // 4 ints * 4 bytes
    }

    @Override
    protected void push(int handle, Vector4Int value) {
        Gdx.gl.glUniform4i(handle, value.x, value.y, value.z, value.w);
    }

    @Override
    public ByteBuffer getByteBuffer() {

        buffer.clear();

        buffer.putInt(value.x);
        buffer.putInt(value.y);
        buffer.putInt(value.z);
        buffer.putInt(value.w);

        buffer.flip();
        return buffer;
    }

    @Override
    public void set(Vector4Int value) {
        this.value.set(value);
    }
}