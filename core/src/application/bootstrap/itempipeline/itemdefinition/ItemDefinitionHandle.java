package application.bootstrap.itempipeline.itemdefinition;

import application.bootstrap.geometrypipeline.mesh.MeshData;
import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import engine.root.HandlePackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

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

    public String getLocalName() {
        return itemDefinitionData.getLocalName();
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

    public String getDisplayName() {
        return itemDefinitionData.getDisplayName();
    }

    public String getDescription() {
        return itemDefinitionData.getDescription();
    }

    public ItemCategory getCategory() {
        return itemDefinitionData.getCategory();
    }

    public float getWeight() {
        return itemDefinitionData.getWeight();
    }

    public boolean isTwoHanded() {
        return itemDefinitionData.isTwoHanded();
    }

    public boolean isSolid() {
        return itemDefinitionData.isSolid();
    }

    public EquipmentType getEquipmentType() {
        return itemDefinitionData.getEquipmentType();
    }

    public float getStat(ItemStat itemStat) {
        return itemDefinitionData.getStat(itemStat);
    }

    public ItemShapeStruct getShape() {
        return itemDefinitionData.getShape();
    }

    public boolean isContainer() {
        return itemDefinitionData.isContainer();
    }

    public ContainerSpaceStruct getContainerSpace() {
        return itemDefinitionData.getContainerSpace();
    }

    public MeshHandle getMeshHandle() {
        return itemDefinitionData.getMeshHandle();
    }

    public boolean hasOpenMesh() {
        return itemDefinitionData.hasOpenMesh();
    }

    public MeshData getOpenMeshData() {
        return itemDefinitionData.getOpenMeshData();
    }

    public MeshData getPocketMeshData() {
        return itemDefinitionData.getPocketMeshData();
    }

    public int getMaterialID() {
        return itemDefinitionData.getMaterialID();
    }

    public short getToolTypeID() {
        return itemDefinitionData.getToolTypeID();
    }

    public boolean isTool() {
        return itemDefinitionData.isTool();
    }

    public int getToolTier() {
        return itemDefinitionData.getToolTier();
    }

    public int getStackSize() {
        return itemDefinitionData.getStackSize();
    }

    public boolean isStackable() {
        return itemDefinitionData.isStackable();
    }

    public short getBlockID() {
        return itemDefinitionData.getBlockID();
    }

    public boolean isBlockPiece() {
        return itemDefinitionData.isBlockPiece();
    }

    // The first action answering this trigger with this item held, struck on this cell of the model grid — null when
    // none does
    public ItemActionStruct findAction(ItemActionTrigger trigger, String heldName, int gridX, int gridY, int gridZ) {

        ObjectArrayList<ItemActionStruct> actions = itemDefinitionData.getActions();

        for (int i = 0; i < actions.size(); i++)
            if (actions.get(i).matches(trigger, heldName, gridX, gridY, gridZ))
                return actions.get(i);

        return null;
    }

    public boolean hasActions() {
        return !itemDefinitionData.getActions().isEmpty();
    }

    public boolean isPickedUpAsOther() {
        return itemDefinitionData.isPickedUpAsOther();
    }

    public String getPickUpAsName() {
        return itemDefinitionData.getPickUpAsName();
    }

    public boolean isSeed() {
        return itemDefinitionData.isSeed();
    }

    public String getPlantsTreeName() {
        return itemDefinitionData.getPlantsTreeName();
    }

    public boolean isSower() {
        return itemDefinitionData.isSower();
    }

    public String getSowsCoveringName() {
        return itemDefinitionData.getSowsCoveringName();
    }

    public boolean isNurturer() {
        return itemDefinitionData.isNurturer();
    }

    public int getNurtureLevels() {
        return itemDefinitionData.getNurtureLevels();
    }
}
