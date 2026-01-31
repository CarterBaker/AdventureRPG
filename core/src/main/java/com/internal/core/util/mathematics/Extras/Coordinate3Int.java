package com.internal.core.util.mathematics.Extras;

import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.util.VertBlockNeighbor3Vector;
import com.internal.core.engine.UtilityPackage;

public final class Coordinate3Int extends UtilityPackage {

    // Internal \\

    private Coordinate3Int() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    // Accessible \\

    // Pack y, z, x into a single int (10-10-10 bits, 2 bits unused) - Y-Z-X order
    // All coordinates: 10 bits each (0-1023)
    public static int pack(int x, int y, int z) {
        return (y << 20) | (z << 10) | x;
    }

    // Direct unpacking from packed coordinate
    // Unpack with sign extension for -512 to 511 range
    public static int unpackX(int packed) {
        int value = packed & 0x3FF; // Get 10 bits
        return (value << 22) >> 22; // Sign extend from bit 9
    }

    public static int unpackY(int packed) {
        int value = (packed >> 20) & 0x3FF; // Get 10 bits
        return (value << 22) >> 22; // Sign extend from bit 9
    }

    public static int unpackZ(int packed) {
        int value = (packed >> 10) & 0x3FF; // Get 10 bits
        return (value << 22) >> 22; // Sign extend from bit 9
    }

    // Java Utility \\

    public static String toString(int value) {
        return "Coordinate3Int(" +
                unpackX(value) + ", " +
                unpackY(value) + ", " +
                unpackZ(value) + ")";
    }
}