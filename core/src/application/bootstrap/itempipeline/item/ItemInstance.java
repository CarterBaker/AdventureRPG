package application.bootstrap.itempipeline.item;

import application.bootstrap.itempipeline.container.ContainerInstance;
import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import engine.root.InstancePackage;

public class ItemInstance extends InstancePackage {

    /*
     * One real item — worn, held, lying in a container, or standing in the
     * world. Handed out by ItemManager. A container item carries its own
     * ContainerInstance, so a backpack or chest keeps what it holds wherever
     * it goes, and its weight includes everything inside it.
     */

    // Internal
    private ItemDefinitionHandle itemDefinitionHandle;

    // Storage — created only for container items
    private ContainerInstance containerInstance;

    // Constructor \\

    public void constructor(ItemDefinitionHandle itemDefinitionHandle) {

        // Internal
        this.itemDefinitionHandle = itemDefinitionHandle;

        // Storage
        if (!itemDefinitionHandle.isContainer())
            return;

        this.containerInstance = create(ContainerInstance.class);
        this.containerInstance.constructor(itemDefinitionHandle.getContainerSize());
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
        return itemDefinitionHandle.getWeight()
                + (containerInstance != null ? containerInstance.getContentWeight() : 0f);
    }
}
