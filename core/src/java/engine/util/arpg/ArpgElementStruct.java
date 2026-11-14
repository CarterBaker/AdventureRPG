package engine.util.arpg;

import engine.root.StructPackage;

public abstract class ArpgElementStruct extends StructPackage {

    /*
     * Base of the ARPG document tree every data file decodes into. A node is
     * an object, an array, a value, or null; typed accessors convert on read
     * and throw a catchable InternalException naming both kinds on mismatch,
     * so ArpgUtility can report the failing file.
     */

    // Type \\

    public boolean isObject() {
        return false;
    }

    public boolean isArray() {
        return false;
    }

    public boolean isValue() {
        return false;
    }

    public boolean isNull() {
        return false;
    }

    // Conversion \\

    public ArpgObjectStruct getAsObject() {
        throw mismatch("an object");
    }

    public ArpgArrayStruct getAsArray() {
        throw mismatch("an array");
    }

    public ArpgValueStruct getAsValue() {
        throw mismatch("a value");
    }

    public String getAsString() {
        throw mismatch("a string");
    }

    public int getAsInt() {
        throw mismatch("an int");
    }

    public long getAsLong() {
        throw mismatch("a long");
    }

    public float getAsFloat() {
        throw mismatch("a float");
    }

    public double getAsDouble() {
        throw mismatch("a double");
    }

    public boolean getAsBoolean() {
        throw mismatch("a boolean");
    }

    // Copy \\

    public abstract ArpgElementStruct deepCopy();

    // Utility \\

    abstract String describe();

    final InternalException mismatch(String expected) {
        return new InternalException("ARPG " + describe() + " cannot be read as " + expected);
    }

    @Override
    public String toString() {
        return ArpgTextUtility.formatCompact(this);
    }
}
