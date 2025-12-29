package com.AdventureRPG.core.shaderpipeline.uniforms;

import java.nio.ByteBuffer;

public abstract class UniformAttribute<T> {

    // Internal
    protected T value;

    // Base \\

    protected UniformAttribute() {
        throw new UnsupportedOperationException(
                "Empty constructor is not allowed. You must provide an initial value.");
    }

    protected UniformAttribute(T value) {
        this.value = value;
    }

    // UBO Utility \\

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

    public abstract void set(T value);
}