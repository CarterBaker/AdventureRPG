package com.internal.bootstrap.shaderpipeline.uniforms.scalarArrays;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;

import java.nio.ByteBuffer;

public final class BooleanArrayUniform extends UniformAttribute<boolean[]> {

    // Internal
    private final int elementCount;
    private final ByteBuffer uboBuffer;

    public BooleanArrayUniform(int elementCount) {

        // Internal
        super(new boolean[elementCount]);
        this.elementCount = elementCount;
        this.uboBuffer = BufferUtils.newByteBuffer(elementCount * 4);
    }

    @Override
    protected void push(int handle, boolean[] data) {

        float[] floatData = new float[elementCount];

        for (int i = 0; i < elementCount; i++)
            floatData[i] = data[i] ? 1f : 0f;

        Gdx.gl.glUniform1fv(handle, elementCount, floatData, 0);
    }

    @Override
    public ByteBuffer getByteBuffer() {

        uboBuffer.clear();

        for (int i = 0; i < elementCount; i++)
            uboBuffer.putFloat(value[i] ? 1f : 0f);

        uboBuffer.flip();
        return uboBuffer;
    }

    @Override
    public void set(boolean[] values) {
        System.arraycopy(values, 0, this.value, 0, elementCount);
    }

    public int elementCount() {
        return elementCount;
    }
}
