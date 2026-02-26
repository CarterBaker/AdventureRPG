package com.internal.bootstrap.shaderpipeline.uniforms.vectorarrays;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.internal.core.util.mathematics.vectors.Vector4;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public final class Vector4ArrayUniform extends UniformAttribute<float[]> {

    // Internal
    private final int elementCount;
    private final ByteBuffer buffer;
    private final FloatBuffer floatBuffer;

    public Vector4ArrayUniform(int elementCount) {
        // Internal
        super(new float[elementCount * 4]);
        this.elementCount = elementCount;
        this.buffer = BufferUtils.newByteBuffer(elementCount * 16); // vec4 = 16 bytes per element, no padding needed
                                                                    // (std140)
        this.floatBuffer = buffer.asFloatBuffer();
    }

    @Override
    public UniformAttribute<?> createDefault() {
        return new Vector4ArrayUniform(elementCount);
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
        if (value instanceof Vector4[] vectors) {
            for (int i = 0; i < vectors.length && i < elementCount; i++) {
                this.value[i * 4] = vectors[i].x;
                this.value[i * 4 + 1] = vectors[i].y;
                this.value[i * 4 + 2] = vectors[i].z;
                this.value[i * 4 + 3] = vectors[i].w;
            }
        } else if (value instanceof com.badlogic.gdx.math.Vector4[] vectors) {
            for (int i = 0; i < vectors.length && i < elementCount; i++) {
                this.value[i * 4] = vectors[i].x;
                this.value[i * 4 + 1] = vectors[i].y;
                this.value[i * 4 + 2] = vectors[i].z;
                this.value[i * 4 + 3] = vectors[i].w;
            }
        } else {
            applyValue((float[]) value);
        }
    }

    public int elementCount() {
        return elementCount;
    }
}