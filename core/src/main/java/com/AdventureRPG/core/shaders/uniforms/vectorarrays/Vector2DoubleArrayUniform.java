package com.AdventureRPG.core.shaders.uniforms.vectorarrays;

import com.AdventureRPG.core.shaders.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Mathematics.Vectors.Vector2Double;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public final class Vector2DoubleArrayUniform extends UniformAttribute<float[]> {

    private static final int COMPONENTS = 2;

    private final int elementCount;

    private final ByteBuffer buffer;
    private final FloatBuffer floatBuffer;

    public Vector2DoubleArrayUniform(int elementCount) {
        super(new float[elementCount * COMPONENTS]);
        this.elementCount = elementCount;

        // Stored as floats for OpenGL
        this.buffer = BufferUtils.newByteBuffer(elementCount * COMPONENTS * 4);
        this.floatBuffer = buffer.asFloatBuffer();
    }

    public void set(Vector2Double[] vectors) {
        int idx = 0;
        for (Vector2Double v : vectors) {
            value[idx++] = (float) v.x;
            value[idx++] = (float) v.y;
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
