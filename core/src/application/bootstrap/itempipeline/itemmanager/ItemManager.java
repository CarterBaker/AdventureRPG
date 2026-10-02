package application.bootstrap.itempipeline.itemmanager;

import application.bootstrap.itempipeline.item.ItemInstance;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.itempipeline.itemdefinitionmanager.ItemDefinitionManager;
import engine.root.ManagerPackage;

public class ItemManager extends ManagerPackage {

    /*
     * The one place real items are made. createItem() turns an item
     * definition into an ItemInstance, giving a container item its own empty
     * container; createStack() makes a stackable item standing for several,
     * and splitStack() parts a stack in two; toCarried() is the item a placed
     * item is picked up as, which may be another one, as an open door is
     * picked up shut. Every item a player is given, finds, splits off, or
     * restores from a save is created here.
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

    public ItemInstance createStack(ItemDefinitionHandle itemDefinitionHandle, int stackCount) {

        ItemInstance itemInstance = createItem(itemDefinitionHandle);
        itemInstance.setStackCount(stackCount);

        return itemInstance;
    }

    // The item a placed item is picked up as — itself, unless its definition names another
    public ItemInstance toCarried(ItemInstance itemInstance) {

        ItemDefinitionHandle itemDefinitionHandle = itemInstance.getItemDefinitionHandle();

        if (!itemDefinitionHandle.isPickedUpAsOther())
            return itemInstance;

        return createItem(itemDefinitionHandle.getPickUpAsName());
    }

    // Takes a count off a stack as a new item of its own — the stack keeps at least one
    public ItemInstance splitStack(ItemInstance itemInstance, int stackCount) {

        if (stackCount < 1 || stackCount >= itemInstance.getStackCount())
            throwException("Cannot split " + stackCount + " off a stack of " + itemInstance.getStackCount()
                    + " '" + itemInstance.getItemDefinitionHandle().getItemName() + "'.");

        itemInstance.setStackCount(itemInstance.getStackCount() - stackCount);

        return createStack(itemInstance.getItemDefinitionHandle(), stackCount);
    }
}
