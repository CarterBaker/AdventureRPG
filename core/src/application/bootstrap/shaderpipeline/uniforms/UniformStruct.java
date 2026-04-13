package application.bootstrap.shaderpipeline.uniforms;

import application.core.engine.StructPackage;

public class UniformStruct<T> extends StructPackage {

    /*
     * Pairs a GL uniform location or UBO byte offset with its typed attribute.
     * A location of -1 means the driver removed the uniform as unused — push()
     * no-ops silently so shared include files never cause crashes on inactive
     * uniforms.
     */

    // Internal
    private final int uniformHandle;
    private final int offset;
    private final UniformAttributeStruct<T> attribute;

    // Constructor — standalone uniform \\

    public UniformStruct(int uniformHandle, UniformAttributeStruct<T> attribute) {
        this(uniformHandle, 0, attribute);
    }

    // Constructor — UBO uniform \\

    public UniformStruct(int uniformHandle, int offset, UniformAttributeStruct<T> attribute) {
        this.uniformHandle = uniformHandle;
        this.offset = offset;
        this.attribute = attribute;
    }

    // Utility \\

    public final void push() {
        if (uniformHandle == -1)
            return;
        attribute.push(uniformHandle);
    }

    public UniformStruct<T> clone() {
        return new UniformStruct<>(uniformHandle, offset, attribute.clone());
    }

    // Accessible \\

    public int getUniformHandle() {
        return uniformHandle;
    }

    public int getOffset() {
        return offset;
    }

    public UniformAttributeStruct<T> attribute() {
        return attribute;
    }
}