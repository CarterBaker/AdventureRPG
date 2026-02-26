package com.internal.bootstrap.shaderpipeline.uniforms.vectorarrays;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.internal.core.util.mathematics.vectors.Vector4Double;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public final class Vector4DoubleArrayUniform extends UniformAttribute<float[]> {

    // Internal
    private final int elementCount;
    private final ByteBuffer buffer;
    private final FloatBuffer floatBuffer;

    public Vector4DoubleArrayUniform(int elementCount) {
        // Internal
        super(new float[elementCount * 4]);
        this.elementCount = elementCount;
        this.buffer = BufferUtils.newByteBuffer(elementCount * 16); // vec4 = 16 bytes per element, no padding needed
                                                                    // (std140, doubles downcast to float for GLSL ES)
        this.floatBuffer = buffer.asFloatBuffer();
    }

    @Override
    public UniformAttribute<?> createDefault() {
        return new Vector4DoubleArrayUniform(elementCount);
    }

    @Override
    protected void push(int handle, float[] data) {
        Gdx.gl.glUniform4fv(handle, elementCount, data, 0);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        floatBuffer.clear();
        floatBuffer.put(value);
        floatBuffer.flip();
        return buffer;
    }

    @Override
    protected void applyValue(float[] value) {
        System.arraycopy(value, 0, this.value, 0, Math.min(value.length, this.value.length));
    }

    @Override
    protected void applyObject(Object value) {
        if (value instanceof Vector4Double[] vectors) {
            for (int i = 0; i < vectors.length && i < elementCount; i++) {
                this.value[i * 4] = (float) vectors[i].x;
                this.value[i * 4 + 1] = (float) vectors[i].y;
                this.value[i * 4 + 2] = (float) vectors[i].z;
                this.value[i * 4 + 3] = (float) vectors[i].w;
            }
        } else {
            applyValue((float[]) value);
        }
    }

    public int elementCount() {
        return elementCount;
    }
}