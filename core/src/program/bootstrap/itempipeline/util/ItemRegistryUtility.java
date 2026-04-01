package program.bootstrap.itempipeline.util;

import program.core.engine.UtilityPackage;
import program.core.settings.EngineSetting;

public class ItemRegistryUtility extends UtilityPackage {

    /*
     * Int ID layout (32 bits):
     * Bits 31-16 upper 16 bits: FNV-1a hash of item name, range [1, 65535]
     * Bits 15- 0 lower 16 bits: 0x0000 — reserved for enchanting encoding
     *
     * When enchanting is wired in:
     * int enchantedID = (baseItemID & 0xFFFF0000) | (enchantValue & 0xFFFF);
     */

    // Internal
    private static final int FNV_OFFSET_BASIS = EngineSetting.FNV_OFFSET_BASIS;
    private static final int FNV_PRIME = EngineSetting.FNV_PRIME;

    public static final int RESERVED_ID = 0;

    // Hashing \\

    public static int toItemIntID(String name) {

        if (name == null || name.isEmpty())
            throwException("Item registry name cannot be null or empty");

        int hash = FNV_OFFSET_BASIS;

        for (int i = 0; i < name.length(); i++) {
            hash ^= name.charAt(i);
            hash *= FNV_PRIME;
        }

        int nameShort = hash & 0xFFFF;

        if (nameShort == 0)
            nameShort = 1;

        return nameShort << 16;
    }

    // Collision Detection \\

    public static boolean isCollision(String incomingName, String existingName, int id) {
        return toItemIntID(incomingName) == id && !incomingName.equals(existingName);
    }
}