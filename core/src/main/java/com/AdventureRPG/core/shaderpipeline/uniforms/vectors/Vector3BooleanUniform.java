package com.AdventureRPG.core.shaderpipeline.uniforms.vectors;

import com.AdventureRPG.core.shaderpipeline.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Vectors.Vector3Boolean;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;

public class Vector3BooleanUniform extends UniformAttribute<Vector3Boolean> {

    private ByteBuffer buffer;

    public Vector3BooleanUniform() {
        super(new Vector3Boolean());
        this.buffer = BufferUtils.newByteBuffer(12); // 3 ints * 4 bytes
    }

    @Override
    protected void push(int handle, Vector3Boolean value) {
        Gdx.gl.glUniform3i(handle, value.x ? 1 : 0, value.y ? 1 : 0, value.z ? 1 : 0);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        buffer.clear();
        buffer.putInt(value.x ? 1 : 0);
        buffer.putInt(value.y ? 1 : 0);
        buffer.putInt(value.z ? 1 : 0);
        buffer.flip();
        return buffer;
    }
}