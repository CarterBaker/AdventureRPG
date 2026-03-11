package com.internal.bootstrap.shaderpipeline.uniforms;

import com.badlogic.gdx.utils.BufferUtils;
import java.nio.ByteBuffer;

public abstract class UniformAttribute<T> {

    // Internal
    private Uniform<?> uniform;
    protected T value;
    private final UniformType type;
    private final int count;
    protected final ByteBuffer uboBuffer;

    // Base \\
    protected UniformAttribute(UniformType type, T defaultValue) {
        this(type, 1, defaultValue);
    }

    protected UniformAttribute(UniformType type, int count, T defaultValue) {
        this.type = type;
        this.count = count;
        this.value = defaultValue;
        this.uboBuffer = BufferUtils.newByteBuffer(computeUBOBufferSize(type, count));
    }

    private static int computeUBOBufferSize(UniformType type, int count) {
        if (count <= 1)
            return type.getStd140Size();
        return UniformUtility.align(type.getStd140Size(), 16) * count;
    }

    // Internal \\
    public void constructor(Uniform<?> uniform) {
        this.uniform = uniform;
    }

    // UBO \\
    public final ByteBuffer getByteBuffer() {
        uboBuffer.clear();
        writeToBuffer(uboBuffer, value);
        uboBuffer.flip();
        return uboBuffer;
    }

    @SuppressWarnings("unchecked")
    protected final void writeToBuffer(ByteBuffer buffer, T value) {
        if (count == 1) {
            type.writeElement(buffer, value);
        } else {
            Object[] elements = (Object[]) value;
            int stride = UniformUtility.align(type.getStd140Size(), 16);
            for (Object el : elements) {
                int start = buffer.position();
                type.writeElement(buffer, el);
                int written = buffer.position() - start;
                int padding = stride - written;
                for (int i = 0; i < padding; i++)
                    buffer.put((byte) 0);
            }
        }
    }

    // Sampler \\
    public boolean isSampler() {
        return false;
    }

    public void bindTexture(int unit) {
    }

    // Push \\
    final void push(int handle) {
        push(handle, value);
    }

    protected abstract void push(int handle, T value);

    // Clone \\
    public abstract UniformAttribute<?> createDefault();

    // Accessible \\
    public final void set(T value) {
        applyValue(value);
    }

    protected abstract void applyValue(T value);

    public final void setObject(Object value) {
        applyObject(value);
    }

    @SuppressWarnings("unchecked")
    protected void applyObject(Object value) {
        set((T) value);
    }

    public T getValue() {
        return value;
    }

    public UniformType getUniformType() {
        return type;
    }

    public int getCount() {
        return count;
    }
}