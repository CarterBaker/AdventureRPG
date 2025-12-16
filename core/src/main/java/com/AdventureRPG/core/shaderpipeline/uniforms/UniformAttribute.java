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

    // Utility \\

    final void push(int handle) {
        push(handle, value);
    }

    protected abstract void push(int handle, T value);

    // UBO Support \\

    /**
     * Write this uniform's data to a ByteBuffer for UBO upload.
     * Default implementation throws - subclasses must override if used in UBOs.
     */
    public ByteBuffer getByteBuffer() {
        throw new UnsupportedOperationException(
                "getByteBuffer() not implemented for " + getClass().getSimpleName());
    }

    // Accessible \\

    public final T get() {
        return value;
    }

    public final void set(T value) {
        this.value = value;
    }
}