package com.AdventureRPG.core.shaderpipeline.uniforms.scalarArrays;

import com.AdventureRPG.core.shaderpipeline.uniforms.UniformAttribute;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;

public final class DoubleArrayUniform extends UniformAttribute<double[]> {

    // Internal
    private final int elementCount;
    private final ByteBuffer uboBuffer;

    public DoubleArrayUniform(int elementCount) {

        // Internal
        super(new double[elementCount]);
        this.elementCount = elementCount;
        this.uboBuffer = BufferUtils.newByteBuffer(elementCount * 4);
    }

    @Override
    protected void push(int handle, double[] data) {

        float[] floatData = new float[elementCount];

        for (int i = 0; i < elementCount; i++)
            floatData[i] = (float) data[i]; // convert to float

        Gdx.gl.glUniform1fv(handle, elementCount, floatData, 0);
    }

    @Override
    public ByteBuffer getByteBuffer() {

        uboBuffer.clear();

        for (int i = 0; i < elementCount; i++)
            uboBuffer.putFloat((float) value[i]); // convert to float

        uboBuffer.flip();
        return uboBuffer;
    }

    @Override
    public void set(double[] values) {
        System.arraycopy(values, 0, this.value, 0, elementCount);
    }

    public int elementCount() {
        return elementCount;
    }
}
