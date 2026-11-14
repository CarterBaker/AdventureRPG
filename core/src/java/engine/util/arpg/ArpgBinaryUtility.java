package engine.util.arpg;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import engine.root.EngineSetting;
import engine.root.EngineUtility;
import engine.root.StructPackage;
import engine.root.UtilityPackage.InternalException;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;

class ArpgBinaryUtility extends EngineUtility {

    /*
     * Encodes the ARPG document tree to its compact binary payload and back.
     * The payload opens with a table of every distinct string, keys and values
     * alike, then the root element: a tag byte followed by its body. Counts,
     * indices and whole numbers are zigzag varints; a decimal is its shortest
     * decimal mantissa and power-of-ten scale, which rebuilds the exact same
     * double in one multiply or divide, and falls back to raw IEEE bits only
     * when it cannot.
     */

    // Internal
    private static final double[] POWERS_OF_TEN = createPowersOfTen();

    // Encode \\

    static byte[] encode(ArpgElementStruct root) {

        Object2IntLinkedOpenHashMap<String> string2Index = new Object2IntLinkedOpenHashMap<>();
        string2Index.defaultReturnValue(EngineSetting.INDEX_NOT_FOUND);
        collectStrings(root, string2Index);

        EncodeBufferStruct buffer = new EncodeBufferStruct();
        buffer.writeVarint(string2Index.size());

        for (String string : string2Index.keySet()) {
            byte[] utf8 = string.getBytes(StandardCharsets.UTF_8);
            buffer.writeVarint(utf8.length);
            buffer.writeBytes(utf8);
        }

        writeElement(root, string2Index, buffer);
        return buffer.toByteArray();
    }

    private static void collectStrings(ArpgElementStruct element, Object2IntLinkedOpenHashMap<String> string2Index) {

        if (element.isObject()) {

            for (Object2ObjectMap.Entry<String, ArpgElementStruct> entry : element.getAsObject().entrySet()) {
                indexString(entry.getKey(), string2Index);
                collectStrings(entry.getValue(), string2Index);
            }

            return;
        }

        if (element.isArray()) {

            for (ArpgElementStruct child : element.getAsArray())
                collectStrings(child, string2Index);

            return;
        }

        if (element.isValue() && element.getAsValue().isString())
            indexString(element.getAsString(), string2Index);
    }

    private static void indexString(String string, Object2IntLinkedOpenHashMap<String> string2Index) {
        if (!string2Index.containsKey(string))
            string2Index.put(string, string2Index.size());
    }

    private static void writeElement(
            ArpgElementStruct element,
            Object2IntLinkedOpenHashMap<String> string2Index,
            EncodeBufferStruct buffer) {

        if (element.isObject()) {

            ArpgObjectStruct object = element.getAsObject();
            buffer.writeByte(EngineSetting.ARPG_TAG_OBJECT);
            buffer.writeVarint(object.size());

            for (Object2ObjectMap.Entry<String, ArpgElementStruct> entry : object.entrySet()) {
                buffer.writeVarint(string2Index.getInt(entry.getKey()));
                writeElement(entry.getValue(), string2Index, buffer);
            }

            return;
        }

        if (element.isArray()) {

            ArpgArrayStruct array = element.getAsArray();
            buffer.writeByte(EngineSetting.ARPG_TAG_ARRAY);
            buffer.writeVarint(array.size());

            for (int i = 0; i < array.size(); i++)
                writeElement(array.get(i), string2Index, buffer);

            return;
        }

        if (element.isNull()) {
            buffer.writeByte(EngineSetting.ARPG_TAG_NULL);
            return;
        }

        writeValue(element.getAsValue(), string2Index, buffer);
    }

    private static void writeValue(
            ArpgValueStruct value,
            Object2IntLinkedOpenHashMap<String> string2Index,
            EncodeBufferStruct buffer) {

        switch (value.getValueType()) {

            case STRING -> {
                buffer.writeByte(EngineSetting.ARPG_TAG_STRING);
                buffer.writeVarint(string2Index.getInt(value.getAsString()));
            }

            case BOOLEAN -> buffer.writeByte(value.getAsBoolean()
                    ? EngineSetting.ARPG_TAG_TRUE
                    : EngineSetting.ARPG_TAG_FALSE);

            case INTEGER -> {
                buffer.writeByte(EngineSetting.ARPG_TAG_INTEGER);
                buffer.writeVarint(value.getAsLong());
            }

            case DECIMAL -> writeDecimal(value.getAsDouble(), buffer);
        }
    }

    private static void writeDecimal(double value, EncodeBufferStruct buffer) {

        BigDecimal decimal = new BigDecimal(ArpgTextUtility.formatDecimal(value)).stripTrailingZeros();
        BigInteger unscaled = decimal.unscaledValue();
        int scale = decimal.scale();

        if (unscaled.bitLength() < Long.SIZE - 1 && Math.abs(scale) <= EngineSetting.ARPG_DECIMAL_MAX_SCALE) {

            long mantissa = unscaled.longValue();
            boolean exact = Math.abs(mantissa) < EngineSetting.ARPG_DECIMAL_MAX_MANTISSA
                    && Double.doubleToRawLongBits(composeDecimal(mantissa, scale)) == Double.doubleToRawLongBits(value);

            if (exact) {
                buffer.writeByte(EngineSetting.ARPG_TAG_DECIMAL);
                buffer.writeVarint(mantissa);
                buffer.writeVarint(scale);
                return;
            }
        }

        buffer.writeByte(EngineSetting.ARPG_TAG_DOUBLE);
        buffer.writeLong(Double.doubleToRawLongBits(value));
    }

    // Decode \\

    static ArpgElementStruct decode(byte[] bytes, int offset) {

        DecodeCursorStruct cursor = new DecodeCursorStruct(bytes, offset);
        String[] strings = new String[cursor.readCount()];

        for (int i = 0; i < strings.length; i++)
            strings[i] = cursor.readString(cursor.readCount());

        ArpgElementStruct root = readElement(cursor, strings);

        if (cursor.hasRemaining())
            throw new InternalException("ARPG payload has unread bytes after its root element");

        return root;
    }

    private static ArpgElementStruct readElement(DecodeCursorStruct cursor, String[] strings) {

        int tag = cursor.readByte();

        return switch (tag) {
            case EngineSetting.ARPG_TAG_OBJECT -> readObject(cursor, strings);
            case EngineSetting.ARPG_TAG_ARRAY -> readArray(cursor, strings);
            case EngineSetting.ARPG_TAG_STRING -> new ArpgValueStruct(readString(cursor, strings));
            case EngineSetting.ARPG_TAG_INTEGER -> new ArpgValueStruct(cursor.readVarint());
            case EngineSetting.ARPG_TAG_DECIMAL -> new ArpgValueStruct(readDecimal(cursor));
            case EngineSetting.ARPG_TAG_DOUBLE -> new ArpgValueStruct(Double.longBitsToDouble(cursor.readLong()));
            case EngineSetting.ARPG_TAG_TRUE -> ArpgValueStruct.TRUE;
            case EngineSetting.ARPG_TAG_FALSE -> ArpgValueStruct.FALSE;
            case EngineSetting.ARPG_TAG_NULL -> ArpgNullStruct.INSTANCE;
            default -> throw new InternalException("ARPG payload holds unknown element tag " + tag);
        };
    }

    private static ArpgObjectStruct readObject(DecodeCursorStruct cursor, String[] strings) {

        int size = cursor.readCount();
        ArpgObjectStruct object = new ArpgObjectStruct(size);

        for (int i = 0; i < size; i++) {

            String key = readString(cursor, strings);

            if (!object.addUnique(key, readElement(cursor, strings)))
                throw new InternalException("ARPG payload repeats object key '" + key + "'");
        }

        return object;
    }

    private static ArpgArrayStruct readArray(DecodeCursorStruct cursor, String[] strings) {

        int size = cursor.readCount();
        ArpgArrayStruct array = new ArpgArrayStruct(size);

        for (int i = 0; i < size; i++)
            array.add(readElement(cursor, strings));

        return array;
    }

    private static String readString(DecodeCursorStruct cursor, String[] strings) {

        long index = cursor.readVarint();

        if (index < 0 || index >= strings.length)
            throw new InternalException("ARPG payload references string " + index + " of " + strings.length);

        return strings[(int) index];
    }

    private static double readDecimal(DecodeCursorStruct cursor) {

        long mantissa = cursor.readVarint();
        long scale = cursor.readVarint();

        if (Math.abs(scale) > EngineSetting.ARPG_DECIMAL_MAX_SCALE
                || Math.abs(mantissa) >= EngineSetting.ARPG_DECIMAL_MAX_MANTISSA)
            throw new InternalException("ARPG payload holds an out-of-range decimal " + mantissa + "e" + -scale);

        return composeDecimal(mantissa, (int) scale);
    }

    // Utility \\

    private static double composeDecimal(long mantissa, int scale) {
        return scale >= 0 ? mantissa / POWERS_OF_TEN[scale] : mantissa * POWERS_OF_TEN[-scale];
    }

    private static double[] createPowersOfTen() {

        double[] powers = new double[EngineSetting.ARPG_DECIMAL_MAX_SCALE + 1];

        for (int i = 0; i < powers.length; i++)
            powers[i] = Math.pow(EngineSetting.ARPG_DECIMAL_BASE, i);

        return powers;
    }

    private static final class EncodeBufferStruct extends StructPackage {

        /*
         * Growable byte buffer the encoder writes tags, varints and raw bytes
         * into.
         */

        // Buffer
        private byte[] bytes;
        private int size;

        // Constructor \\

        EncodeBufferStruct() {
            this.bytes = new byte[EngineSetting.ARPG_BUFFER_INITIAL_CAPACITY];
            this.size = 0;
        }

        // Write \\

        void writeByte(int value) {
            ensureCapacity(1);
            bytes[size++] = (byte) value;
        }

        void writeBytes(byte[] values) {
            ensureCapacity(values.length);
            System.arraycopy(values, 0, bytes, size, values.length);
            size += values.length;
        }

        void writeVarint(long value) {

            long zigzag = (value << 1) ^ (value >> EngineSetting.ARPG_VARINT_MAX_SHIFT);

            while ((zigzag & ~EngineSetting.ARPG_VARINT_PAYLOAD_MASK) != 0) {
                writeByte((int) (zigzag & EngineSetting.ARPG_VARINT_PAYLOAD_MASK)
                        | EngineSetting.ARPG_VARINT_CONTINUE_BIT);
                zigzag >>>= EngineSetting.ARPG_VARINT_SHIFT;
            }

            writeByte((int) zigzag);
        }

        void writeLong(long value) {

            int shift = Long.SIZE - EngineSetting.ARPG_BYTE_BITS;

            while (shift >= 0) {
                writeByte((int) (value >>> shift) & EngineSetting.ARPG_BYTE_MASK);
                shift -= EngineSetting.ARPG_BYTE_BITS;
            }
        }

        // Utility \\

        private void ensureCapacity(int additional) {

            if (size + additional <= bytes.length)
                return;

            int capacity = Math.max(bytes.length * EngineSetting.SCRATCH_BUFFER_GROWTH_FACTOR, size + additional);
            this.bytes = Arrays.copyOf(bytes, capacity);
        }

        byte[] toByteArray() {
            return Arrays.copyOf(bytes, size);
        }
    }

    private static final class DecodeCursorStruct extends StructPackage {

        /*
         * Read position over one decoded payload. Every read is bounds
         * checked, so a short or malformed payload fails with a clear message
         * rather than an index error.
         */

        // Payload
        private final byte[] bytes;
        private int position;

        // Constructor \\

        DecodeCursorStruct(byte[] bytes, int offset) {
            this.bytes = bytes;
            this.position = offset;
        }

        // Read \\

        int readByte() {

            if (position >= bytes.length)
                throw new InternalException("ARPG payload ended in the middle of an element");

            return bytes[position++] & EngineSetting.ARPG_BYTE_MASK;
        }

        long readVarint() {

            long zigzag = 0;
            int shift = 0;

            while (shift <= EngineSetting.ARPG_VARINT_MAX_SHIFT) {

                int current = readByte();
                zigzag |= (long) (current & EngineSetting.ARPG_VARINT_PAYLOAD_MASK) << shift;

                if ((current & EngineSetting.ARPG_VARINT_CONTINUE_BIT) == 0)
                    return (zigzag >>> 1) ^ -(zigzag & 1);

                shift += EngineSetting.ARPG_VARINT_SHIFT;
            }

            throw new InternalException("ARPG payload holds a varint longer than 64 bits");
        }

        int readCount() {

            long count = readVarint();

            if (count < 0 || count > bytes.length - position)
                throw new InternalException("ARPG payload declares " + count + " items with only "
                        + (bytes.length - position) + " bytes left");

            return (int) count;
        }

        long readLong() {

            long value = 0;

            for (int i = 0; i < Long.BYTES; i++)
                value = (value << EngineSetting.ARPG_BYTE_BITS) | readByte();

            return value;
        }

        String readString(int length) {

            String string = new String(bytes, position, length, StandardCharsets.UTF_8);
            position += length;
            return string;
        }

        boolean hasRemaining() {
            return position < bytes.length;
        }
    }
}
