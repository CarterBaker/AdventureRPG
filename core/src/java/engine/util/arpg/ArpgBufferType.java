package engine.util.arpg;

import engine.root.EngineSetting;

public enum ArpgBufferType {

    /*
     * The component types a typed array holds, each with the name the text form
     * writes and the bytes one value takes. A type's place in this list is the
     * code stored in the file, so new types are only ever added at the end.
     */

    FLOAT32("float32", Float.BYTES, 0L, 0L),
    INT32("int32", Integer.BYTES, Integer.MIN_VALUE, Integer.MAX_VALUE),
    UINT16("uint16", Short.BYTES, 0L, EngineSetting.ARPG_SHORT_MASK),
    UINT8("uint8", Byte.BYTES, 0L, EngineSetting.ARPG_BYTE_MASK),
    INT16("int16", Short.BYTES, Short.MIN_VALUE, Short.MAX_VALUE),
    INT8("int8", Byte.BYTES, Byte.MIN_VALUE, Byte.MAX_VALUE);

    // Internal
    private static final ArpgBufferType[] VALUES = values();
    private final String textName;
    private final int valueBytes;
    private final long minimum;
    private final long maximum;

    // Constructor \\

    ArpgBufferType(String textName, int valueBytes, long minimum, long maximum) {

        // Internal
        this.textName = textName;
        this.valueBytes = valueBytes;
        this.minimum = minimum;
        this.maximum = maximum;
    }

    // Lookup \\

    static ArpgBufferType fromCode(int code) {
        return code >= 0 && code < VALUES.length ? VALUES[code] : null;
    }

    static ArpgBufferType fromTextName(String textName) {

        for (ArpgBufferType type : VALUES)
            if (type.textName.equals(textName))
                return type;

        return null;
    }

    // Accessible \\

    public String getTextName() {
        return textName;
    }

    public int getValueBytes() {
        return valueBytes;
    }

    public boolean isFloat() {
        return this == FLOAT32;
    }

    public boolean holds(long value) {
        return value >= minimum && value <= maximum;
    }
}
