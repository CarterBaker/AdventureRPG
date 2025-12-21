package com.AdventureRPG.core.shaders.uniforms.vectorarrays;

import com.AdventureRPG.core.shaders.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Mathematics.Vectors.Vector2;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public final class Vector2ArrayUniform extends UniformAttribute<float[]> {

    private static final int COMPONENTS = 2;

    private final int elementCount;

    private final ByteBuffer buffer;
    private final FloatBuffer floatBuffer;

    public Vector2ArrayUniform(int elementCount) {
        super(new float[elementCount * COMPONENTS]);
        this.elementCount = elementCount;

        this.buffer = BufferUtils.newByteBuffer(elementCount * COMPONENTS * 4);
        this.floatBuffer = buffer.asFloatBuffer();
    }

    public void set(Vector2[] vectors) {
        int idx = 0;
        for (Vector2 v : vectors) {
            value[idx++] = v.x;
            value[idx++] = v.y;
        }
    }

    public void set(com.badlogic.gdx.math.Vector2[] vectors) {
        int idx = 0;
        for (com.badlogic.gdx.math.Vector2 v : vectors) {
            value[idx++] = v.x;
            value[idx++] = v.y;
        }
    }

    @Override
    protected void push(int handle, float[] data) {
        Gdx.gl.glUniform2fv(handle, elementCount, data, 0);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        floatBuffer.clear();
        floatBuffer.put(value);
        floatBuffer.flip();
        return buffer;
    }

    public int elementCount() {
        return elementCount;
    }
}
