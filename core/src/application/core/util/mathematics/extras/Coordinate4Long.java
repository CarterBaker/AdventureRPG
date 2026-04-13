package application.core.util.mathematics.extras;

public final class Coordinate4Long {

    // Internal \\

    private Coordinate4Long() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    // Layout: [X 16bits][Y 16bits][Z 16bits][W 16bits]
    // Each component: signed 16-bit, range -32768 to 32767

    // Pack \\

    public static long pack(int x, int y, int z, int w) {
        return ((long) (x & 0xFFFF) << 48)
                | ((long) (y & 0xFFFF) << 32)
                | ((long) (z & 0xFFFF) << 16)
                | ((long) (w & 0xFFFF));
    }

    // Unpack — sign extended \\

    public static int unpackX(long packed) {
        return (int) (packed >> 48);
    }

    public static int unpackY(long packed) {
        return (int) ((packed << 16) >> 48);
    }

    public static int unpackZ(long packed) {
        return (int) ((packed << 32) >> 48);
    }

    public static int unpackW(long packed) {
        return (int) ((packed << 48) >> 48);
    }

    // Java Utility \\

    public static String toString(long packed) {
        return "Coordinate4Long(" +
                unpackX(packed) + ", " +
                unpackY(packed) + ", " +
                unpackZ(packed) + ", " +
                unpackW(packed) + ")";
    }
}