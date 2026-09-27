package engine.util.arpg;

public final class ArpgNullStruct extends ArpgElementStruct {

    /*
     * The single explicit null of the ARPG document tree. Shared by every
     * document, since it carries no state.
     */

    // Internal
    public static final ArpgNullStruct INSTANCE = new ArpgNullStruct();

    // Constructor \\

    private ArpgNullStruct() {
    }

    // Type \\

    @Override
    public boolean isNull() {
        return true;
    }

    // Copy \\

    @Override
    public ArpgNullStruct deepCopy() {
        return this;
    }

    // Utility \\

    @Override
    String describe() {
        return "null";
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ArpgNullStruct;
    }

    @Override
    public int hashCode() {
        return ArpgNullStruct.class.hashCode();
    }
}
