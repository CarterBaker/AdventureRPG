package application.bootstrap.itempipeline.util;

import engine.root.EngineSetting;
import engine.root.EngineUtility;

public class ItemRegistryUtility extends EngineUtility {

    /*
     * Builds item IDs. The upper 16 bits hold the FNV-1a hash of the item name
     * in [1, 65535]; the lower 16 bits are zero and reserved for enchantment
     * values.
     */

    // Internal
    private static final int FNV_OFFSET_BASIS = EngineSetting.FNV_OFFSET_BASIS;
    private static final int FNV_PRIME = EngineSetting.FNV_PRIME;

    public static final int RESERVED_ID = EngineSetting.REGISTRY_RESERVED_ID;

    // Naming \\

    public static String toItemName(String definitionName, String localName) {
        return definitionName + "/" + localName;
    }

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