package application.bootstrap.itempipeline.item;

import application.bootstrap.itempipeline.container.ContainerInstance;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import engine.root.InstancePackage;

public class ItemInstance extends InstancePackage {

    /*
     * One real item — worn, held, lying in a container, or standing in the
     * world. Handed out by ItemManager. A container item carries its own
     * ContainerInstance, so a backpack or chest keeps what it holds wherever
     * it goes, and its weight includes everything inside it. A stackable item
     * stands for a whole stack of its kind, up to its stack size; absorb() is
     * the one path that moves count from one stack into another, and a stack
     * absorbed down to nothing is spent and dropped by whoever held it.
     */

    // Internal
    private ItemDefinitionHandle itemDefinitionHandle;

    // Stack
    private int stackCount;

    // Storage — created only for container items
    private ContainerInstance containerInstance;

    // Constructor \\

    public void constructor(ItemDefinitionHandle itemDefinitionHandle) {

        // Internal
        this.itemDefinitionHandle = itemDefinitionHandle;

        // Stack
        this.stackCount = 1;

        // Storage
        if (!itemDefinitionHandle.isContainer())
            return;

        this.containerInstance = create(ContainerInstance.class);
        this.containerInstance.constructor(itemDefinitionHandle.getContainerSpace().getSize());
    }

    // Stack \\

    public boolean stacksWith(ItemInstance itemInstance) {
        return itemInstance != this
                && itemDefinitionHandle.isStackable()
                && itemInstance.getItemDefinitionHandle() == itemDefinitionHandle;
    }

    // Moves as much of another stack of the same item into this one as fits, returning how many moved
    public int absorb(ItemInstance itemInstance) {

        if (!stacksWith(itemInstance))
            throwException("Item '" + itemInstance.getItemDefinitionHandle().getItemName()
                    + "' cannot stack onto '" + itemDefinitionHandle.getItemName() + "'.");

        int moved = Math.min(getStackRoom(), itemInstance.stackCount);

        stackCount += moved;
        itemInstance.stackCount -= moved;

        return moved;
    }

    public void setStackCount(int stackCount) {

        if (stackCount < 1 || stackCount > itemDefinitionHandle.getStackSize())
            throwException("Item '" + itemDefinitionHandle.getItemName() + "' cannot stack " + stackCount
                    + " — its stack holds 1 to " + itemDefinitionHandle.getStackSize() + ".");

        this.stackCount = stackCount;
    }

    public int getStackCount() {
        return stackCount;
    }

    public int getStackRoom() {
        return itemDefinitionHandle.getStackSize() - stackCount;
    }

    public boolean isSpent() {
        return stackCount == 0;
    }

    // Accessible \\

    public ItemDefinitionHandle getItemDefinitionHandle() {
        return itemDefinitionHandle;
    }

    public boolean hasContainer() {
        return containerInstance != null;
    }

    public ContainerInstance getContainerInstance() {
        return containerInstance;
    }

    public float getTotalWeight() {
        return itemDefinitionHandle.getWeight() * stackCount
                + (containerInstance != null ? containerInstance.getContentWeight() : 0f);
    }
}
