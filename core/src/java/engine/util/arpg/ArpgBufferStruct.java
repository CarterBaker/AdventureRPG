package engine.util.arpg;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import engine.root.EngineSetting;
import engine.root.UtilityPackage.InternalException;

public final class ArpgBufferStruct extends ArpgElementStruct {

    /*
     * A typed array in the ARPG tree: one component type and its values as raw
     * little-endian bytes. A buffer decoded from a file is a read-only view
     * into that file's bytes, so its values are read in place without being
     * copied; one made for writing wraps the bytes packed from the caller's
     * array. Float values read from any type, whole values only from the
     * integer types, unsigned types never as negatives.
     */

    // Internal
    private final ArpgBufferType bufferType;
    private final ByteBuffer data;
    private final int valueCount;

    // Constructor \\

    ArpgBufferStruct(ArpgBufferType bufferType, ByteBuffer data) {

        // Internal
        this.bufferType = bufferType;
        this.data = data.slice().order(ByteOrder.LITTLE_ENDIAN).asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN);
        this.valueCount = this.data.limit() / bufferType.getValueBytes();
    }

    public ArpgBufferStruct(float[] values, int count) {
        this(ArpgBufferType.FLOAT32, packFloats(values, count));
    }

    public ArpgBufferStruct(ArpgBufferType bufferType, int[] values, int count) {
        this(bufferType, packInts(bufferType, values, count));
    }

    private static ByteBuffer packFloats(float[] values, int count) {

        ByteBuffer packed = ByteBuffer.allocate(count * Float.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        packed.asFloatBuffer().put(values, 0, count);

        return packed;
    }

    private static ByteBuffer packInts(ArpgBufferType bufferType, int[] values, int count) {

        if (bufferType.isFloat())
            throw new InternalException("ARPG float32 buffer cannot be made from whole values");

        int valueBytes = bufferType.getValueBytes();
        ByteBuffer packed = ByteBuffer.allocate(count * valueBytes).order(ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < count; i++) {

            if (!bufferType.holds(values[i]))
                throw new InternalException("ARPG " + bufferType.getTextName() + " buffer cannot hold " + values[i]);

            writeWhole(packed, i * valueBytes, valueBytes, values[i]);
        }

        return packed;
    }

    private static void writeWhole(ByteBuffer packed, int offset, int valueBytes, int value) {

        if (valueBytes == Integer.BYTES)
            packed.putInt(offset, value);
        else if (valueBytes == Short.BYTES)
            packed.putShort(offset, (short) value);
        else
            packed.put(offset, (byte) value);
    }

    // Type \\

    @Override
    public boolean isBuffer() {
        return true;
    }

    @Override
    public ArpgBufferStruct getAsBuffer() {
        return this;
    }

    // Accessible \\

    public ArpgBufferType getBufferType() {
        return bufferType;
    }

    public int size() {
        return valueCount;
    }

    public float getFloat(int index) {
        return bufferType.isFloat() ? data.getFloat(index * Float.BYTES) : getInt(index);
    }

    public int getInt(int index) {

        return switch (bufferType) {
            case INT32 -> data.getInt(index * Integer.BYTES);
            case UINT16 -> data.getShort(index * Short.BYTES) & EngineSetting.ARPG_SHORT_MASK;
            case INT16 -> data.getShort(index * Short.BYTES);
            case UINT8 -> data.get(index) & EngineSetting.ARPG_BYTE_MASK;
            case INT8 -> data.get(index);
            case FLOAT32 -> throw mismatch("whole values");
        };
    }

    // The values' raw little-endian bytes, read in place
    public ByteBuffer getData() {
        return data.duplicate().order(ByteOrder.LITTLE_ENDIAN);
    }

    // Copy \\

    @Override
    public ArpgBufferStruct deepCopy() {

        ByteBuffer copy = ByteBuffer.allocate(data.limit());
        copy.put(data.duplicate()).flip();

        return new ArpgBufferStruct(bufferType, copy);
    }

    // Utility \\

    @Override
    String describe() {
        return bufferType.getTextName() + " buffer";
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof ArpgBufferStruct buffer
                && bufferType == buffer.bufferType
                && data.equals(buffer.data));
    }

    @Override
    public int hashCode() {
        return bufferType.hashCode() * 31 + data.hashCode();
    }
}
