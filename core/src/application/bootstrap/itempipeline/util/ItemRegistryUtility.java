package application.bootstrap.itempipeline.util;

import engine.root.EngineSetting;
import engine.root.EngineUtility;

public class ItemRegistryUtility extends EngineUtility {

    /*
     * Builds item names and IDs. The upper 16 bits of an ID hold the FNV-1a
     * hash of the item name in [1, 65535]; the lower 16 bits are zero and
     * reserved for enchantment values. A block piece is named after its block
     * behind a reserved prefix no item file can produce, so its name alone
     * says which block it builds.
     */

    // Internal
    private static final int FNV_OFFSET_BASIS = EngineSetting.FNV_OFFSET_BASIS;
    private static final int FNV_PRIME = EngineSetting.FNV_PRIME;

    public static final int RESERVED_ID = EngineSetting.REGISTRY_RESERVED_ID;

    // Naming \\

    public static String toItemName(String definitionName, String localName) {
        return definitionName + "/" + localName;
    }

    public static String toBlockPieceName(String blockName) {
        return EngineSetting.BLOCK_PIECE_NAME_PREFIX + blockName;
    }

    public static boolean isBlockPieceName(String itemName) {
        return itemName.startsWith(EngineSetting.BLOCK_PIECE_NAME_PREFIX);
    }

    public static String toBlockName(String blockPieceName) {
        return blockPieceName.substring(EngineSetting.BLOCK_PIECE_NAME_PREFIX.length());
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