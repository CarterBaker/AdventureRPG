package com.AdventureRPG.core.shaderpipeline.uniforms.vectors;

import com.AdventureRPG.core.shaderpipeline.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.mathematics.vectors.Vector4Boolean;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import java.nio.ByteBuffer;

public class Vector4BooleanUniform extends UniformAttribute<Vector4Boolean> {

    // Internal
    private final ByteBuffer buffer;

    public Vector4BooleanUniform() {

        // Internal
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

    @Override
    public void set(Vector4Boolean value) {
        this.value.set(value);
    }
}