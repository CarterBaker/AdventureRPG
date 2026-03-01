package com.internal.bootstrap.shaderpipeline.uniforms;

/*
 * Pairs a GL uniform location (or UBO byte offset) with its typed attribute.
 * A location of -1 means the driver removed the uniform as unused — push()
 * no-ops silently so shared include files never cause crashes on inactive uniforms.
 */
public class Uniform<T> {

    // Internal
    public final int uniformHandle;
    public final int offset;
    public final UniformAttribute<T> attribute;

    // Internal \\

    // Standalone uniform
    public Uniform(int uniformHandle, UniformAttribute<T> attribute) {
        this(uniformHandle, 0, attribute);
    }

    // UBO uniform (with byte offset)
    public Uniform(int uniformHandle, int offset, UniformAttribute<T> attribute) {
        this.uniformHandle = uniformHandle;
        this.offset = offset;
        this.attribute = attribute;
        attribute.constructor(this);
    }

    // Utility \\

    public final void push() {
        if (uniformHandle == -1)
            return;
        attribute.push(uniformHandle);
    }

    // Accessible \\

    public UniformAttribute<T> attribute() {
        return attribute;
    }
}