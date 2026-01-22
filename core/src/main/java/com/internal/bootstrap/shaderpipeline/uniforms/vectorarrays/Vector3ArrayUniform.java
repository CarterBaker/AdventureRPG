package com.internal.bootstrap.shaderpipeline.uniforms.vectorarrays;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.internal.core.util.mathematics.vectors.Vector3;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public final class Vector3ArrayUniform extends UniformAttribute<float[]> {

    // Internal
    private final int elementCount;
    private final ByteBuffer uboBuffer;
    private final float[] uniformData;
    private final FloatBuffer uniformBuffer;

    public Vector3ArrayUniform(int elementCount) {

        // Internal
        super(new float[elementCount * 4]);
        this.elementCount = elementCount;
        this.uboBuffer = BufferUtils.newByteBuffer(elementCount * 16);
        this.uniformData = new float[elementCount * 3];
        this.uniformBuffer = uboBuffer.asFloatBuffer();
    }

    @Override
    public UniformAttribute<?> createDefault() {
        return new Vector3ArrayUniform(elementCount);
    }

    @Override
    protected void push(int handle, float[] data) {
        Gdx.gl.glUniform3fv(handle, elementCount, uniformData, 0);
    }

    @Override
    public ByteBuffer getByteBuffer() {

        uniformBuffer.clear();

        // write each vec3 with padding
        for (int i = 0; i < elementCount; i++) {
            uniformBuffer.put(value[i * 4]); // x
            uniformBuffer.put(value[i * 4 + 1]); // y
            uniformBuffer.put(value[i * 4 + 2]); // z
            uniformBuffer.put(0f); // padding
        }

        uniformBuffer.flip();
        return uboBuffer;
    }

    public void set(Vector3[] vectors) {

        for (int i = 0; i < vectors.length && i < elementCount; i++) {

            // padded array for UBO
            value[i * 4] = vectors[i].x;
            value[i * 4 + 1] = vectors[i].y;
            value[i * 4 + 2] = vectors[i].z;
            value[i * 4 + 3] = 0f; // padding

            // tightly packed array for uniform upload
            uniformData[i * 3] = vectors[i].x;
            uniformData[i * 3 + 1] = vectors[i].y;
            uniformData[i * 3 + 2] = vectors[i].z;
        }

        super.set(value);
    }

    public void set(com.badlogic.gdx.math.Vector3[] vectors) {

        for (int i = 0; i < vectors.length && i < elementCount; i++) {

            value[i * 4] = vectors[i].x;
            value[i * 4 + 1] = vectors[i].y;
            value[i * 4 + 2] = vectors[i].z;
            value[i * 4 + 3] = 0f;

            uniformData[i * 3] = vectors[i].x;
            uniformData[i * 3 + 1] = vectors[i].y;
            uniformData[i * 3 + 2] = vectors[i].z;
        }

        super.set(value);
    }

    @Override
    public void set(float[] value) {

        System.arraycopy(value, 0, this.value, 0, Math.min(value.length, this.value.length));

        // update uniformData without padding
        for (int i = 0; i < elementCount; i++) {
            uniformData[i * 3] = this.value[i * 4];
            uniformData[i * 3 + 1] = this.value[i * 4 + 1];
            uniformData[i * 3 + 2] = this.value[i * 4 + 2];
        }

        super.set(value);
    }

    public int elementCount() {
        return elementCount;
    }
}
