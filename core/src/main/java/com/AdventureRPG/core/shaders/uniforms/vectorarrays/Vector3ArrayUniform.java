package com.AdventureRPG.core.shaders.uniforms.vectorarrays;

import com.AdventureRPG.core.shaders.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Mathematics.Vectors.Vector3;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public final class Vector3ArrayUniform extends UniformAttribute<float[]> {

    private static final int COMPONENTS = 3;

    private final int elementCount;

    private final ByteBuffer buffer;
    private final FloatBuffer floatBuffer;

    public Vector3ArrayUniform(int elementCount) {
        super(new float[elementCount * COMPONENTS]);
        this.elementCount = elementCount;

        // std140: vec3 occupies 12 bytes, padding handled by UBO stride
        this.buffer = BufferUtils.newByteBuffer(elementCount * COMPONENTS * 4);
        this.floatBuffer = buffer.asFloatBuffer();
    }

    public void set(Vector3[] vectors) {
        int idx = 0;
        for (Vector3 v : vectors) {
            value[idx++] = v.x;
            value[idx++] = v.y;
            value[idx++] = v.z;
        }
    }

    public void set(com.badlogic.gdx.math.Vector3[] vectors) {
        int idx = 0;
        for (com.badlogic.gdx.math.Vector3 v : vectors) {
            value[idx++] = v.x;
            value[idx++] = v.y;
            value[idx++] = v.z;
        }
    }

    @Override
    protected void push(int handle, float[] data) {
        // elementCount, NOT data.length
        Gdx.gl.glUniform3fv(handle, elementCount, data, 0);
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
