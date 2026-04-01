package program.bootstrap.itempipeline.itemdefinition;

import program.bootstrap.geometrypipeline.mesh.MeshHandle;
import program.core.engine.HandlePackage;

public class ItemDefinitionHandle extends HandlePackage {

    /*
     * Persistent reference to a loaded item definition. Registered and owned
     * by ItemDefinitionManager. Delegates all accessors through
     * ItemDefinitionData.
     */

    // Internal
    private ItemDefinitionData itemDefinitionData;

    // Constructor \\

    public void constructor(ItemDefinitionData itemDefinitionData) {

        // Internal
        this.itemDefinitionData = itemDefinitionData;
    }

    // Accessible \\

    public ItemDefinitionData getItemDefinitionData() {
        return itemDefinitionData;
    }

    public String getItemName() {
        return itemDefinitionData.getItemName();
    }

    public int getItemID() {
        return itemDefinitionData.getItemID();
    }

    public short getNameShort() {
        return (short) ((itemDefinitionData.getItemID() >> 16) & 0xFFFF);
    }

    public short getEnchantShort() {
        return (short) (itemDefinitionData.getItemID() & 0xFFFF);
    }

    public float getWeight() {
        return itemDefinitionData.getWeight();
    }

    public boolean isTwoHanded() {
        return itemDefinitionData.isTwoHanded();
    }

    public boolean isBackpack() {
        return itemDefinitionData.isBackpack();
    }

    public MeshHandle getMeshHandle() {
        return itemDefinitionData.getMeshHandle();
    }

    public int getMaterialID() {
        return itemDefinitionData.getMaterialID();
    }
}