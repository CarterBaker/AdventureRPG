package engine.util.arpg;

import java.util.Arrays;

public final class ArpgValueStruct extends ArpgElementStruct {

    /*
     * Immutable scalar of the ARPG document tree: a string, a boolean, a whole
     * number, a decimal, or a raw block of bytes. Numbers convert between
     * widths on read, strings parse on numeric reads, and every value reads as
     * its text; bytes read as their base64 behind the reserved text marker.
     * Floats are widened through their shortest decimal, so 0.1f stores as
     * 0.1. A bytes value owns its array, which no caller may change.
     */

    // Shared
    static final ArpgValueStruct TRUE = new ArpgValueStruct(true);
    static final ArpgValueStruct FALSE = new ArpgValueStruct(false);

    // Value
    private final ArpgValueType valueType;
    private final String stringValue;
    private final boolean booleanValue;
    private final long integerValue;
    private final double decimalValue;
    private final byte[] bytesValue;

    // Constructor \\

    public ArpgValueStruct(String value) {
        this(ArpgValueType.STRING, requireString(value), false, 0L, 0.0, null);
    }

    public ArpgValueStruct(boolean value) {
        this(ArpgValueType.BOOLEAN, null, value, 0L, 0.0, null);
    }

    public ArpgValueStruct(long value) {
        this(ArpgValueType.INTEGER, null, false, value, 0.0, null);
    }

    public ArpgValueStruct(float value) {
        this(ArpgValueType.DECIMAL, null, false, 0L, requireFinite(widenFloat(value)), null);
    }

    public ArpgValueStruct(double value) {
        this(ArpgValueType.DECIMAL, null, false, 0L, requireFinite(value), null);
    }

    public ArpgValueStruct(byte[] value) {
        this(ArpgValueType.BYTES, null, false, 0L, 0.0, requireBytes(value));
    }

    private ArpgValueStruct(
            ArpgValueType valueType,
            String stringValue,
            boolean booleanValue,
            long integerValue,
            double decimalValue,
            byte[] bytesValue) {

        // Value
        this.valueType = valueType;
        this.stringValue = stringValue;
        this.booleanValue = booleanValue;
        this.integerValue = integerValue;
        this.decimalValue = decimalValue;
        this.bytesValue = bytesValue;
    }

    private static String requireString(String value) {

        if (value == null)
            throw new InternalException("ARPG string values cannot be null");

        return value;
    }

    private static byte[] requireBytes(byte[] value) {

        if (value == null)
            throw new InternalException("ARPG bytes values cannot be null");

        return value;
    }

    private static double requireFinite(double value) {

        if (!Double.isFinite(value))
            throw new InternalException("ARPG decimal values must be finite, found " + value);

        return value;
    }

    private static double widenFloat(float value) {

        double decimal = Double.parseDouble(Float.toString(value));
        return (float) decimal == value ? decimal : value;
    }

    // Type \\

    @Override
    public boolean isValue() {
        return true;
    }

    public boolean isString() {
        return valueType == ArpgValueType.STRING;
    }

    public boolean isBoolean() {
        return valueType == ArpgValueType.BOOLEAN;
    }

    public boolean isNumber() {
        return valueType == ArpgValueType.INTEGER || valueType == ArpgValueType.DECIMAL;
    }

    public boolean isInteger() {
        return valueType == ArpgValueType.INTEGER;
    }

    public boolean isBytes() {
        return valueType == ArpgValueType.BYTES;
    }

    // Conversion \\

    @Override
    public ArpgValueStruct getAsValue() {
        return this;
    }

    @Override
    public String getAsString() {

        return switch (valueType) {
            case STRING -> stringValue;
            case BOOLEAN -> Boolean.toString(booleanValue);
            case INTEGER -> Long.toString(integerValue);
            case DECIMAL -> ArpgTextUtility.formatDecimal(decimalValue);
            case BYTES -> ArpgTextUtility.formatBytes(bytesValue);
        };
    }

    @Override
    public int getAsInt() {

        return switch (valueType) {
            case INTEGER -> (int) integerValue;
            case DECIMAL -> (int) decimalValue;
            default -> Integer.parseInt(getAsString());
        };
    }

    @Override
    public long getAsLong() {

        return switch (valueType) {
            case INTEGER -> integerValue;
            case DECIMAL -> (long) decimalValue;
            default -> Long.parseLong(getAsString());
        };
    }

    @Override
    public float getAsFloat() {

        return switch (valueType) {
            case INTEGER -> integerValue;
            case DECIMAL -> (float) decimalValue;
            default -> Float.parseFloat(getAsString());
        };
    }

    @Override
    public double getAsDouble() {

        return switch (valueType) {
            case INTEGER -> integerValue;
            case DECIMAL -> decimalValue;
            default -> Double.parseDouble(getAsString());
        };
    }

    @Override
    public boolean getAsBoolean() {
        return valueType == ArpgValueType.BOOLEAN ? booleanValue : Boolean.parseBoolean(getAsString());
    }

    @Override
    public byte[] getAsBytes() {

        if (valueType != ArpgValueType.BYTES)
            throw mismatch("bytes");

        return bytesValue;
    }

    // Copy \\

    @Override
    public ArpgValueStruct deepCopy() {
        return this;
    }

    // Utility \\

    ArpgValueType getValueType() {
        return valueType;
    }

    @Override
    String describe() {

        if (valueType == ArpgValueType.BYTES)
            return "bytes value of " + bytesValue.length + " bytes";

        return valueType.name().toLowerCase() + " value '" + getAsString() + "'";
    }

    @Override
    public boolean equals(Object other) {

        if (this == other)
            return true;

        if (!(other instanceof ArpgValueStruct value))
            return false;

        if (isNumber() && value.isNumber())
            return isInteger() && value.isInteger()
                    ? integerValue == value.integerValue
                    : Double.compare(getAsDouble(), value.getAsDouble()) == 0;

        if (valueType != value.valueType)
            return false;

        return switch (valueType) {
            case STRING -> stringValue.equals(value.stringValue);
            case BYTES -> Arrays.equals(bytesValue, value.bytesValue);
            default -> booleanValue == value.booleanValue;
        };
    }

    @Override
    public int hashCode() {

        return switch (valueType) {
            case STRING -> stringValue.hashCode();
            case BOOLEAN -> Boolean.hashCode(booleanValue);
            case INTEGER -> Long.hashCode(integerValue);
            case DECIMAL -> decimalValue == (long) decimalValue
                    ? Long.hashCode((long) decimalValue)
                    : Double.hashCode(decimalValue);
            case BYTES -> Arrays.hashCode(bytesValue);
        };
    }
}
