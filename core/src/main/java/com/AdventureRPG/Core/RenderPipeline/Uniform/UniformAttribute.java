package com.AdventureRPG.core.renderpipeline.uniform;

public abstract class UniformAttribute<T> {

    // Internal
    protected T value;

    // Base \\

    protected UniformAttribute() {
        throw new UnsupportedOperationException( // TODO: Add my own error
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

    // Accessible \\

    public final T get() {
        return value;
    }

    public final void set(T value) {
        this.value = value;
    }
}