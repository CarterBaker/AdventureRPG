package com.internal.bootstrap.shaderpipeline.uniforms.vectorarrays;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.internal.core.util.mathematics.vectors.Vector3Boolean;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public final class Vector3BooleanArrayUniform extends UniformAttribute<int[]> {

    // Internal
    private final int elementCount;
    private final ByteBuffer uboBuffer;
    private final int[] uniformData;
    private final IntBuffer uniformBuffer;

    public Vector3BooleanArrayUniform(int elementCount) {
        // Internal
        super(new int[elementCount * 4]); // padded: 3 ints + 1 padding per element
        this.elementCount = elementCount;
        this.uboBuffer = BufferUtils.newByteBuffer(elementCount * 16); // bvec3 padded to 16 bytes per element (std140)
        this.uniformData = new int[elementCount * 3]; // tightly packed for GL uniform upload
        this.uniformBuffer = uboBuffer.asIntBuffer();
    }

    @Override
    public UniformAttribute<?> createDefault() {
        return new Vector3BooleanArrayUniform(elementCount);
    }

    @Override
    protected void push(int handle, int[] data) {
        Gdx.gl.glUniform3iv(handle, elementCount, uniformData, 0);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        uniformBuffer.clear();
        for (int i = 0; i < elementCount; i++) {
            uniformBuffer.put(value[i * 4]); // x
            uniformBuffer.put(value[i * 4 + 1]); // y
            uniformBuffer.put(value[i * 4 + 2]); // z
            uniformBuffer.put(0); // padding
        }
        uniformBuffer.flip();
        return uboBuffer;
    }

    @Override
    protected void applyValue(int[] value) {
        System.arraycopy(value, 0, this.value, 0, Math.min(value.length, this.value.length));
        for (int i = 0; i < elementCount; i++) {
            uniformData[i * 3] = this.value[i * 4];
            uniformData[i * 3 + 1] = this.value[i * 4 + 1];
            uniformData[i * 3 + 2] = this.value[i * 4 + 2];
        }
    }

    @Override
    protected void applyObject(Object value) {
        if (value instanceof Vector3Boolean[] vectors) {
            for (int i = 0; i < vectors.length && i < elementCount; i++) {
                this.value[i * 4] = vectors[i].x ? 1 : 0;
                this.value[i * 4 + 1] = vectors[i].y ? 1 : 0;
                this.value[i * 4 + 2] = vectors[i].z ? 1 : 0;
                this.value[i * 4 + 3] = 0;
                uniformData[i * 3] = vectors[i].x ? 1 : 0;
                uniformData[i * 3 + 1] = vectors[i].y ? 1 : 0;
                uniformData[i * 3 + 2] = vectors[i].z ? 1 : 0;
            }
        } else {
            applyValue((int[]) value);
        }
    }

    public int elementCount() {
        return elementCount;
    }
}