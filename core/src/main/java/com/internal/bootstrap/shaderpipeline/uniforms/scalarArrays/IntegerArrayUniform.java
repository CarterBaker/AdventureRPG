package com.internal.bootstrap.shaderpipeline.uniforms.scalarArrays;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;

import java.nio.ByteBuffer;

public final class IntegerArrayUniform extends UniformAttribute<int[]> {

    // Internal
    private final int elementCount;
    private final ByteBuffer uboBuffer;

    private final float[] elements;

    public IntegerArrayUniform(int elementCount) {

        // Internal
        super(new int[elementCount]);
        this.elementCount = elementCount;
        this.uboBuffer = BufferUtils.newByteBuffer(elementCount * 4);

        this.elements = new float[elementCount];
    }

    @Override
    protected void push(int handle, int[] data) {

        for (int i = 0; i < elementCount; i++)
            elements[i] = data[i];

        Gdx.gl.glUniform1fv(handle, elementCount, elements, 0);
    }

    @Override
    public ByteBuffer getByteBuffer() {

        uboBuffer.clear();

        for (int i = 0; i < elementCount; i++)
            uboBuffer.putInt(value[i]);

        uboBuffer.flip();
        return uboBuffer;
    }

    @Override
    public void set(int[] values) {

        for (int i = 0; i < elementCount; i++)
            elements[i] = values[i];

        super.set(values);
    }

    public int elementCount() {
        return elementCount;
    }
}