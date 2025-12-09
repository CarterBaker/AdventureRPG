package com.AdventureRPG.core.shaderpipeline.uniform;

public class Uniform<T> {

    // Internal
    public final int uniformHandle;
    public final UniformAttribute<T> attribute;

    // Base \\

    public Uniform(
            int uniformHandle,
            UniformAttribute<T> attribute) {

        // Internal
        this.uniformHandle = uniformHandle;
        this.attribute = attribute;
    }

    // Utiity \\

    public final void push() {
        attribute.push(uniformHandle);
    }

    // Accessible \\

    public UniformAttribute<?> getAttribute() {
        return attribute;
    }
}
