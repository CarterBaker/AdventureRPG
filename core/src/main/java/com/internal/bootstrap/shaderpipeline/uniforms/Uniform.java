package com.internal.bootstrap.shaderpipeline.uniforms;

public class Uniform<T> {

    // Internal
    public final int uniformHandle; // Used for standalone uniforms
    public final int offset; // Used for UBO uniforms
    public final UniformAttribute<T> attribute;

    // Constructor for standalone uniforms
    public Uniform(
            int uniformHandle,
            UniformAttribute<T> attribute) {
        this(uniformHandle, 0, attribute);
        attribute.constructor(this);
    }

    // Constructor for UBO uniforms (with offset)
    public Uniform(
            int uniformHandle,
            int offset,
            UniformAttribute<T> attribute) {
        this.uniformHandle = uniformHandle;
        this.offset = offset;
        this.attribute = attribute;
        attribute.constructor(this);
    }

    // Utility \\
    public final void push() {
        attribute.push(uniformHandle);
    }

    // Accessible \\
    public UniformAttribute<T> attribute() {
        return attribute;
    }
}