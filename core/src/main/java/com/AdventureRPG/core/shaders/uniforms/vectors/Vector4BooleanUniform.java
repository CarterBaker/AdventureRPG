package com.AdventureRPG.core.shaders.uniforms.vectors;

import com.AdventureRPG.core.shaders.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Mathematics.Vectors.Vector4Boolean;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;

public class Vector4BooleanUniform extends UniformAttribute<Vector4Boolean> {

    private ByteBuffer buffer;

    public Vector4BooleanUniform() {
        super(new Vector4Boolean());
        this.buffer = BufferUtils.newByteBuffer(16); // 4 ints * 4 bytes
    }

    @Override
    protected void push(int handle, Vector4Boolean value) {
        Gdx.gl.glUniform4i(handle,
                value.x ? 1 : 0,
                value.y ? 1 : 0,
                value.z ? 1 : 0,
                value.w ? 1 : 0);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        buffer.clear();
        buffer.putInt(value.x ? 1 : 0);
        buffer.putInt(value.y ? 1 : 0);
        buffer.putInt(value.z ? 1 : 0);
        buffer.putInt(value.w ? 1 : 0);
        buffer.flip();
        return buffer;
    }
}