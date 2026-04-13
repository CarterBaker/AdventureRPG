package program.core.util;

import program.core.engine.EngineUtility;
import program.core.settings.EngineSetting;

/*
 * Hashing helpers for converting stable string names into numeric registry IDs.
 * Uses FNV-1a to guarantee the same name always produces the same ID across
 * runs, machines, and load orders — making IDs safe to persist in save files.
 */
public class RegistryUtility extends EngineUtility {

    // Constants \\

    private static final int FNV_OFFSET_BASIS = EngineSetting.FNV_OFFSET_BASIS;
    private static final int FNV_PRIME = EngineSetting.FNV_PRIME;

    public static final short RESERVED_ID = EngineSetting.REGISTRY_RESERVED_ID;

    // Hashing \\

    /*
     * Converts a registry name into a stable short ID in the range [1, 32767].
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

        short id = (short) (hash & 0x7FFF);

        return id == RESERVED_ID ? 1 : id;
    }

    /*
     * Converts a registry name into a stable int ID.
     * Used for systems that need a larger ID range than short allows.
     * 0 is reserved — if the hash resolves to 0 it is remapped to 1.
     */
    public static int toIntID(String name) {

        if (name == null || name.isEmpty())
            throwException("Registry name cannot be null or empty");

        int hash = FNV_OFFSET_BASIS;

        for (int i = 0; i < name.length(); i++) {
            hash ^= name.charAt(i);
            hash *= FNV_PRIME;
        }

        return hash == 0 ? 1 : hash;
    }

    // Collision Detection \\

    public static boolean isCollision(String incomingName, String existingName, short id) {
        return toShortID(incomingName) == id && !incomingName.equals(existingName);
    }

    public static boolean isCollision(String incomingName, String existingName, int id) {
        return toIntID(incomingName) == id && !incomingName.equals(existingName);
    }
}