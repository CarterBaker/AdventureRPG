package application.bootstrap.shaderpipeline.uniforms;

import java.nio.ByteBuffer;

import engine.root.StructPackage;
import engine.util.memory.BufferUtils;

public abstract class UniformAttributeStruct<T> extends StructPackage {

    /*
     * Typed value container for a single uniform. Holds the current value,
     * knows how to write it to a ByteBuffer for UBO upload, and knows how to
     * push it to a GL uniform location. Subclasses implement push and clone.
     */

    // Internal
    protected T value;
    private final UniformType type;
    private final int count;
    protected final ByteBuffer uboBuffer;

    // Constructor — single \\

    protected UniformAttributeStruct(UniformType type, T defaultValue) {
        this(type, 1, defaultValue);
    }

    // Constructor — array \\

    protected UniformAttributeStruct(UniformType type, int count, T defaultValue) {
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

    // Clone \\
    public abstract UniformAttributeStruct<?> createDefault();

    @SuppressWarnings("unchecked")
    public UniformAttributeStruct<T> clone() {
        UniformAttributeStruct<T> copy = (UniformAttributeStruct<T>) createDefault();
        copy.applyValue(value);
        return copy;
    }

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