package com.internal.bootstrap.shaderpipeline.uniforms.vectorarrays;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.internal.core.util.mathematics.vectors.Vector2;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public final class Vector2ArrayUniform extends UniformAttribute<float[]> {

    // Internal
    private final int elementCount;
    private final ByteBuffer buffer;
    private final FloatBuffer floatBuffer;

    public Vector2ArrayUniform(int elementCount) {
        // Internal
        super(new float[elementCount * 2]);
        this.elementCount = elementCount;
        this.buffer = BufferUtils.newByteBuffer(elementCount * 16); // vec2 padded to 16 bytes per element (std140)
        this.floatBuffer = buffer.asFloatBuffer();
    }

    @Override
    public UniformAttribute<?> createDefault() {
        return new Vector2ArrayUniform(elementCount);
    }

    @Override
    protected void push(int handle, float[] data) {
        Gdx.gl.glUniform2fv(handle, elementCount, data, 0);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        floatBuffer.clear();
        for (int i = 0; i < elementCount; i++) {
            floatBuffer.put(value[i * 2]); // x
            floatBuffer.put(value[i * 2 + 1]); // y
            floatBuffer.put(0f); // padding
            floatBuffer.put(0f); // padding
        }
        floatBuffer.flip();
        return buffer;
    }

    @Override
    protected void applyValue(float[] value) {
        System.arraycopy(value, 0, this.value, 0, Math.min(value.length, this.value.length));
    }

    @Override
    protected void applyObject(Object value) {
        if (value instanceof Vector2[] vectors) {
            for (int i = 0; i < vectors.length && i < elementCount; i++) {
                this.value[i * 2] = vectors[i].x;
                this.value[i * 2 + 1] = vectors[i].y;
            }
        } else if (value instanceof com.badlogic.gdx.math.Vector2[] vectors) {
            for (int i = 0; i < vectors.length && i < elementCount; i++) {
                this.value[i * 2] = vectors[i].x;
                this.value[i * 2 + 1] = vectors[i].y;
            }
        } else {
            applyValue((float[]) value);
        }
    }

    public int elementCount() {
        return elementCount;
    }
}