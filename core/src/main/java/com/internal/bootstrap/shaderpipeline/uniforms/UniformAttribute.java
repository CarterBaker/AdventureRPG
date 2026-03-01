package com.internal.bootstrap.shaderpipeline.uniforms;

import java.nio.ByteBuffer;

public abstract class UniformAttribute<T> {

    // Internal
    private Uniform<?> uniform;
    protected T value;

    public void constructor(Uniform<?> uniform) {
        this.uniform = uniform;
    }
    // Internal \\

    // Base \\

    protected UniformAttribute() {
        throw new UnsupportedOperationException(
                "Empty constructor is not allowed. You must provide an initial value.");
    }

    protected UniformAttribute(T value) {
        this.value = value;
    }

    // UBO Utility \\

    public abstract UniformAttribute<?> createDefault();

    public ByteBuffer getByteBuffer() {
        throw new UnsupportedOperationException(
                "getByteBuffer() not implemented for " + getClass().getSimpleName());
    }

    // Sampler Utility \\

    public boolean isSampler() {
        return false;
    }

    public void bindTexture(int unit) {
        // No-op for non-samplers
    }

    // Uniform Utility \\

    final void push(int handle) {
        push(handle, value);
    }

    protected abstract void push(int handle, T value);

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
}