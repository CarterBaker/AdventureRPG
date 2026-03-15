package com.internal.bootstrap.entitypipeline.inventory;

import com.internal.bootstrap.itempipeline.backpack.BackpackInstance;
import com.internal.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import com.internal.core.engine.HandlePackage;

public class InventoryHandle extends HandlePackage {

    /*
     * Per-entity inventory state. Holds main hand, off hand, and backpack
     * references. All fields default to null — no backpack is assigned until
     * explicitly set. No manager owns this — it lives directly on EntityInstance.
     */

    // Items
    private ItemDefinitionHandle mainHand;
    private ItemDefinitionHandle offHand;

    // Storage
    private BackpackInstance backpack;

    // Accessible \\

    public ItemDefinitionHandle getMainHand() {
        return mainHand;
    }

    public void setMainHand(ItemDefinitionHandle mainHand) {
        this.mainHand = mainHand;
    }

    public boolean hasMainHand() {
        return mainHand != null;
    }

    public ItemDefinitionHandle getOffHand() {
        return offHand;
    }

    public void setOffHand(ItemDefinitionHandle offHand) {
        this.offHand = offHand;
    }

    public boolean hasOffHand() {
        return offHand != null;
    }

    public BackpackInstance getBackpack() {
        return backpack;
    }

    public void setBackpack(BackpackInstance backpack) {
        this.backpack = backpack;
    }

    public boolean hasBackpack() {
        return backpack != null;
    }
}