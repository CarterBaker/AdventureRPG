package com.internal.bootstrap.shaderpipeline.uniforms.scalarArrays;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;

import java.nio.ByteBuffer;

public final class BooleanArrayUniform extends UniformAttribute<boolean[]> {

    // Internal
    private final int elementCount;
    private final ByteBuffer uboBuffer;

    private final float[] elements;

    public BooleanArrayUniform(int elementCount) {

        // Internal
        super(new boolean[elementCount]);
        this.elementCount = elementCount;
        this.uboBuffer = BufferUtils.newByteBuffer(elementCount * 4);

        this.elements = new float[elementCount];
    }

    @Override
    protected void push(int handle, boolean[] data) {

        for (int i = 0; i < elementCount; i++)
            elements[i] = data[i] ? 1f : 0f;

        Gdx.gl.glUniform1fv(handle, elementCount, elements, 0);
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

        for (int i = 0; i < elementCount; i++)
            elements[i] = values[i] ? 1f : 0f;

        super.set(values);
    }

    public int elementCount() {
        return elementCount;
    }
}
