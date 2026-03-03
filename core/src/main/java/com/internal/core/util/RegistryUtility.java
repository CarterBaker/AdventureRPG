package com.internal.core.util;

import com.internal.core.engine.UtilityPackage;
import com.internal.core.engine.settings.EngineSetting;

/*
 * Hashing helpers for converting stable string names into numeric registry IDs.
 * Uses FNV-1a to guarantee the same name always produces the same ID across
 * runs, machines, and load orders — making IDs safe to persist in save files.
 */
public class RegistryUtility extends UtilityPackage {

    // Constants \\

    private static final int FNV_OFFSET_BASIS = EngineSetting.FNV_OFFSET_BASIS;
    private static final int FNV_PRIME = EngineSetting.FNV_PRIME;

    // ID 0 is reserved as a sentinel (air / null block)
    public static final short RESERVED_ID = EngineSetting.REGISTRY_RESERVED_ID;

    // Hashing \\

    /*
     * Converts a registry name (e.g. "grass", "stone_brick") into a stable
     * short ID in the range [1, 32767]. The same name will always produce the
     * same ID regardless of load order or session.
     *
     * ID 0 is reserved — if the hash resolves to 0 it is remapped to 1.
     */
    public static short toShortID(String name) {
        if (name == null || name.isEmpty())
            throwException("Registry name cannot be null or empty");

        int hash = FNV_OFFSET_BASIS;
        for (int i = 0; i < name.length(); i++) {
            hash ^= name.charAt(i);
            hash *= FNV_PRIME;
        }

        // Mask to positive short range [0, 32767], then reserve 0
        short id = (short) (hash & 0x7FFF);
        return id == RESERVED_ID ? 1 : id;
    }

    /*
     * Validates that two names do not hash to the same ID.
     * Call this inside addBlock() at startup to catch collisions immediately.
     */
    public static boolean isCollision(String incomingName, String existingName, short id) {
        return toShortID(incomingName) == id && !incomingName.equals(existingName);
    }
}