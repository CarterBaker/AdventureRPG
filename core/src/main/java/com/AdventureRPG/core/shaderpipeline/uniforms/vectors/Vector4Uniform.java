package com.AdventureRPG.core.shaderpipeline.uniforms.vectors;

import com.AdventureRPG.core.shaderpipeline.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Vectors.Vector4;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;

public class Vector4Uniform extends UniformAttribute<Vector4> {

    private ByteBuffer buffer;

    public Vector4Uniform() {
        super(new Vector4());
        this.buffer = BufferUtils.newByteBuffer(16); // 4 floats * 4 bytes
    }

    @Override
    protected void push(int handle, Vector4 value) {
        Gdx.gl.glUniform4f(handle, value.x, value.y, value.z, value.w);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        buffer.clear();
        buffer.putFloat(value.x);
        buffer.putFloat(value.y);
        buffer.putFloat(value.z);
        buffer.putFloat(value.w);
        buffer.flip();
        return buffer;
    }
}