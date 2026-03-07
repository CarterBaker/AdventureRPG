package com.internal.bootstrap.entitypipeline.inventory;

import com.internal.bootstrap.itempipeline.backpack.BackpackInstance;
import com.internal.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import com.internal.core.engine.HandlePackage;

public class InventoryHandle extends HandlePackage {

    // Slots
    private ItemDefinitionHandle mainHand;
    private ItemDefinitionHandle offHand;
    private BackpackInstance backpack;

    // Constructor \\

    public void constructor() {
        this.mainHand = null;
        this.offHand = null;
        this.backpack = null;
    }

    // Accessible \\

    public ItemDefinitionHandle getMainHand() {
        return mainHand;
    }

    public ItemDefinitionHandle getOffHand() {
        return offHand;
    }

    public BackpackInstance getBackpack() {
        return backpack;
    }

    public void setMainHand(ItemDefinitionHandle item) {
        this.mainHand = item;
    }

    public void setOffHand(ItemDefinitionHandle item) {
        this.offHand = item;
    }

    public void setBackpack(BackpackInstance backpack) {
        this.backpack = backpack;
    }

    public boolean hasMainHand() {
        return mainHand != null;
    }

    public boolean hasOffHand() {
        return offHand != null;
    }

    public boolean hasBackpack() {
        return backpack != null;
    }
}