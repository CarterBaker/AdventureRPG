package com.internal.bootstrap.shaderpipeline.uniforms;

import java.nio.ByteBuffer;

public abstract class UniformAttribute<T> {

    // Internal
    private Uniform<?> uniform;
    protected T value;

    public void constructor(Uniform<?> uniform) {
        this.uniform = uniform;
    }

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

    // Uniform Utility \\

    final void push(int handle) {
        push(handle, value);
    }

    protected abstract void push(int handle, T value);

    // Accessible \\

    public void set(T value) {
        uniform.set();
    }
}