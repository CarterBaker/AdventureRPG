package com.internal.bootstrap.itempipeline.itemdefinitionmanager;

import com.internal.core.engine.DataPackage;

public class ItemDefinitionData extends DataPackage {

    // Identity

    private String itemName;

    /*
     * Int ID layout (32 bits):
     * [ bits 31-16 ] upper 16 bits = FNV-derived name hash, range [1, 65535]
     * [ bits 15- 0 ] lower 16 bits = 0x0000 reserved for enchanting encoding later
     */
    private int itemID;

    // Constructor \

    public void constructor(String itemName, int itemID) {
        this.itemName = itemName;
        this.itemID = itemID;
    }

    // Accessible \

    public String getItemName() {
        return itemName;
    }

    public int getItemID() {
        return itemID;
    }

    /** Upper 16 bits — name-derived half. */
    public short getNameShort() {
        return (short) ((itemID >> 16) & 0xFFFF);
    }

    /** Lower 16 bits — enchanting half, currently 0. */
    public short getEnchantShort() {
        return (short) (itemID & 0xFFFF);
    }

}