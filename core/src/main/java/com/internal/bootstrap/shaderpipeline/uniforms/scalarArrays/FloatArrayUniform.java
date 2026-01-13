package com.internal.bootstrap.shaderpipeline.uniforms.scalarArrays;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;

import java.nio.ByteBuffer;

public final class FloatArrayUniform extends UniformAttribute<float[]> {

    // Internal
    private final int elementCount;
    private final ByteBuffer uboBuffer;

    public FloatArrayUniform(int elementCount) {

        // Internal
        super(new float[elementCount]);
        this.elementCount = elementCount;
        this.uboBuffer = BufferUtils.newByteBuffer(elementCount * 4);
    }

    @Override
    protected void push(int handle, float[] data) {
        Gdx.gl.glUniform1fv(handle, elementCount, data, 0);
    }

    @Override
    public ByteBuffer getByteBuffer() {

        uboBuffer.clear();

        for (int i = 0; i < elementCount; i++)
            uboBuffer.putFloat(value[i]);

        uboBuffer.flip();
        return uboBuffer;
    }

    @Override
    public void set(float[] values) {
        System.arraycopy(values, 0, this.value, 0, elementCount);
    }

    public int elementCount() {
        return elementCount;
    }
}