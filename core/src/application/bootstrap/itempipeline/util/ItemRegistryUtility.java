package application.bootstrap.itempipeline.util;

import engine.root.EngineSetting;
import engine.root.EngineUtility;

public class ItemRegistryUtility extends EngineUtility {

    /*
     * Builds item names and IDs. ItemDefinitionManager assigns every item a
     * sequential index in registration order; an ID holds that index in its
     * upper 16 bits, and the lower 16 bits are zero and reserved for
     * enchantment values. A block piece is named after its block behind a
     * reserved prefix no item file can produce, so its name alone says which
     * block it builds.
     */

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

    // IDs \\

    public static int toItemID(int itemIndex) {
        return itemIndex << EngineSetting.REGISTRY_ITEM_ID_SHIFT;
    }

    public static int toItemIndex(int itemID) {
        return itemID >>> EngineSetting.REGISTRY_ITEM_ID_SHIFT;
    }
}
