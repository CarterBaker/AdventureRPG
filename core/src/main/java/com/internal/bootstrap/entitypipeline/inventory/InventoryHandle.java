package com.internal.bootstrap.entitypipeline.inventory;

import com.internal.bootstrap.itempipeline.backpack.BackpackInstance;
import com.internal.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import com.internal.core.engine.HandlePackage;

public class InventoryHandle extends HandlePackage {

    private ItemDefinitionHandle mainHand;
    private ItemDefinitionHandle offHand;
    private BackpackInstance backpack;

    @Override
    protected void create() {
        // Backpack always present — no null checks needed downstream
        this.backpack = create(BackpackInstance.class);
    }

    public void constructor() {
        this.mainHand = null;
        this.offHand = null;
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

    public boolean hasMainHand() {
        return mainHand != null;
    }

    public boolean hasOffHand() {
        return offHand != null;
    }
}