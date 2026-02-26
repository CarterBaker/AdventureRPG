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
        super(new float[elementCount * 4]); // padded: 3 floats + 1 padding per element
        this.elementCount = elementCount;
        this.uboBuffer = BufferUtils.newByteBuffer(elementCount * 16); // vec3 padded to 16 bytes per element (std140)
        this.uniformData = new float[elementCount * 3]; // tightly packed for GL uniform upload
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
        for (int i = 0; i < elementCount; i++) {
            uniformBuffer.put(value[i * 4]); // x
            uniformBuffer.put(value[i * 4 + 1]); // y
            uniformBuffer.put(value[i * 4 + 2]); // z
            uniformBuffer.put(0f); // padding
        }
        uniformBuffer.flip();
        return uboBuffer;
    }

    @Override
    protected void applyValue(float[] value) {
        System.arraycopy(value, 0, this.value, 0, Math.min(value.length, this.value.length));
        for (int i = 0; i < elementCount; i++) {
            uniformData[i * 3] = this.value[i * 4];
            uniformData[i * 3 + 1] = this.value[i * 4 + 1];
            uniformData[i * 3 + 2] = this.value[i * 4 + 2];
        }
    }

    @Override
    protected void applyObject(Object value) {
        if (value instanceof Vector3[] vectors) {
            for (int i = 0; i < vectors.length && i < elementCount; i++) {
                this.value[i * 4] = vectors[i].x;
                this.value[i * 4 + 1] = vectors[i].y;
                this.value[i * 4 + 2] = vectors[i].z;
                this.value[i * 4 + 3] = 0f;
                uniformData[i * 3] = vectors[i].x;
                uniformData[i * 3 + 1] = vectors[i].y;
                uniformData[i * 3 + 2] = vectors[i].z;
            }
        } else if (value instanceof com.badlogic.gdx.math.Vector3[] vectors) {
            for (int i = 0; i < vectors.length && i < elementCount; i++) {
                this.value[i * 4] = vectors[i].x;
                this.value[i * 4 + 1] = vectors[i].y;
                this.value[i * 4 + 2] = vectors[i].z;
                this.value[i * 4 + 3] = 0f;
                uniformData[i * 3] = vectors[i].x;
                uniformData[i * 3 + 1] = vectors[i].y;
                uniformData[i * 3 + 2] = vectors[i].z;
            }
        } else {
            applyValue((float[]) value);
        }
    }

    public int elementCount() {
        return elementCount;
    }
}