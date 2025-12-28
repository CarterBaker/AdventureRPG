package com.AdventureRPG.core.shaders.uniforms.vectors;

import com.AdventureRPG.core.shaders.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Mathematics.Vectors.Vector3Boolean;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import java.nio.ByteBuffer;

public class Vector3BooleanUniform extends UniformAttribute<Vector3Boolean> {

    // Internal
    private final ByteBuffer buffer;

    public Vector3BooleanUniform() {

        // Internal
        super(new Vector3Boolean());
        this.buffer = BufferUtils.newByteBuffer(16);
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
        buffer.putInt(0); // padding

        buffer.flip();
        return buffer;
    }

    @Override
    public void set(Vector3Boolean value) {
        this.value.set(value);
    }
}