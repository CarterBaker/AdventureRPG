package com.AdventureRPG.core.shaderpipeline.uniforms.vectors;

import com.AdventureRPG.core.shaderpipeline.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Vectors.Vector2Boolean;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;

public class Vector2BooleanUniform extends UniformAttribute<Vector2Boolean> {

    private ByteBuffer buffer;

    public Vector2BooleanUniform() {
        super(new Vector2Boolean());
        this.buffer = BufferUtils.newByteBuffer(8); // 2 ints * 4 bytes
    }

    @Override
    protected void push(int handle, Vector2Boolean value) {
        Gdx.gl.glUniform2i(handle, value.x ? 1 : 0, value.y ? 1 : 0);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        buffer.clear();
        buffer.putInt(value.x ? 1 : 0);
        buffer.putInt(value.y ? 1 : 0);
        buffer.flip();
        return buffer;
    }
}