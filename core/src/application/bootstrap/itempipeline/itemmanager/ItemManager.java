package application.bootstrap.itempipeline.itemmanager;

import application.bootstrap.itempipeline.item.ItemInstance;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.itempipeline.itemdefinitionmanager.ItemDefinitionManager;
import engine.root.ManagerPackage;

public class ItemManager extends ManagerPackage {

    /*
     * The one place real items are made. createItem() turns an item
     * definition into an ItemInstance, giving a container item its own empty
     * container. Every item a player is given, finds, or restores from a save
     * is created here.
     */

    // Internal
    private ItemDefinitionManager itemDefinitionManager;

    // Base \\

    @Override
    protected void get() {
        this.itemDefinitionManager = get(ItemDefinitionManager.class);
    }

    // Creation \\

    public ItemInstance createItem(ItemDefinitionHandle itemDefinitionHandle) {

        ItemInstance itemInstance = create(ItemInstance.class);
        itemInstance.constructor(itemDefinitionHandle);

        return itemInstance;
    }

    public ItemInstance createItem(String itemName) {
        return createItem(itemDefinitionManager.getItemHandleFromItemName(itemName));
    }
}
