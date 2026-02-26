package com.internal.bootstrap.shaderpipeline.uniforms.scalarArrays;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;

import java.nio.ByteBuffer;

public final class BooleanArrayUniform extends UniformAttribute<boolean[]> {

    // Internal
    private final int elementCount;
    private final ByteBuffer uboBuffer;
    private final int[] elements; // converted for GL upload

    public BooleanArrayUniform(int elementCount) {
        // Internal
        super(new boolean[elementCount]);
        this.elementCount = elementCount;
        this.uboBuffer = BufferUtils.newByteBuffer(elementCount * 4); // 1 int * 4 bytes per element
        this.elements = new int[elementCount];
    }

    @Override
    public UniformAttribute<?> createDefault() {
        return new BooleanArrayUniform(elementCount);
    }

    @Override
    protected void push(int handle, boolean[] data) {
        Gdx.gl.glUniform1iv(handle, elementCount, elements, 0);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        uboBuffer.clear();
        for (int i = 0; i < elementCount; i++)
            uboBuffer.putInt(value[i] ? 1 : 0);
        uboBuffer.flip();
        return uboBuffer;
    }

    @Override
    protected void applyValue(boolean[] values) {
        System.arraycopy(values, 0, this.value, 0, Math.min(values.length, this.value.length));
        for (int i = 0; i < elementCount; i++)
            elements[i] = this.value[i] ? 1 : 0;
    }

    public int elementCount() {
        return elementCount;
    }
}