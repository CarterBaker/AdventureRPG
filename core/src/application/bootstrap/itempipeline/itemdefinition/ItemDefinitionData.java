package application.bootstrap.itempipeline.itemdefinition;

import application.bootstrap.geometrypipeline.mesh.MeshData;
import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import engine.root.DataPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ItemDefinitionData extends DataPackage {

    /*
     * Immutable item definition payload loaded from ARPG. Holds identity,
     * presentation, physical properties, the body slot it is worn in, the
     * statistics it grants, the sub-voxel shape it takes up in a container
     * and in the world, whether its rough box stops whoever walks into it,
     * and render references for one item type. An item with a container space
     * holds its own container of that space. A container with a lid carries
     * its model without the lid, drawn in the world while it stands open, and
     * a pocket carries the box of walls its space is shown in, drawn only in
     * the inventory's menus. A tool names the tool type it is and the highest
     * break tier it can break; a stackable item holds up to its stack size in
     * one item; a block piece names the block it builds. An item may carry
     * actions that turn it into another item where it stands, and may be
     * picked up as another item, as an open door is picked up shut. A seed
     * names the tree it grows into once planted. An item may sow a covering
     * over the block it is used on and nurture what covers it by a number of
     * levels, as grass seed sows grass and a moisture pouch feeds moss. Owned
     * by ItemDefinitionHandle for the engine lifetime.
     */

    // Identity
    private final String itemName;
    private final String localName;
    private final int itemID;

    // Presentation
    private final String displayName;
    private final String description;
    private final ItemCategory category;

    // Properties
    private final float weight;
    private final boolean twoHanded;
    private final boolean solid;

    // Equipment
    private final EquipmentType equipmentType;
    private final float[] stats;

    // Storage
    private final ItemShapeStruct shape;
    private final ContainerSpaceStruct containerSpace;

    // Render
    private final MeshHandle meshHandle;
    private final MeshData openMeshData;
    private final MeshData pocketMeshData;
    private final int materialID;

    // Tool
    private final short toolTypeID;
    private final int toolTier;

    // Stacking
    private final int stackSize;

    // Block — the block a block piece builds, BLOCK_PIECE_NONE for every other item
    private final short blockID;

    // Actions
    private final ObjectArrayList<ItemActionStruct> actions;
    private final String pickUpAsName;
    private final String plantsTreeName;

    // Coverage — the covering a sower lays, ITEM_SOWS_NONE for none, and the levels it adds where it is used
    private final String sowsCoveringName;
    private final int nurtureLevels;

    // Constructor \\

    public ItemDefinitionData(
            String itemName,
            String localName,
            int itemID,
            String displayName,
            String description,
            ItemCategory category,
            float weight,
            boolean twoHanded,
            boolean solid,
            EquipmentType equipmentType,
            float[] stats,
            ItemShapeStruct shape,
            ContainerSpaceStruct containerSpace,
            MeshHandle meshHandle,
            MeshData openMeshData,
            MeshData pocketMeshData,
            int materialID,
            short toolTypeID,
            int toolTier,
            int stackSize,
            short blockID,
            ObjectArrayList<ItemActionStruct> actions,
            String pickUpAsName,
            String plantsTreeName,
            String sowsCoveringName,
            int nurtureLevels) {

        // Identity
        this.itemName = itemName;
        this.localName = localName;
        this.itemID = itemID;

        // Presentation
        this.displayName = displayName;
        this.description = description;
        this.category = category;

        // Properties
        this.weight = weight;
        this.twoHanded = twoHanded;
        this.solid = solid;

        // Equipment
        this.equipmentType = equipmentType;
        this.stats = stats;

        // Storage
        this.shape = shape;
        this.containerSpace = containerSpace;

        // Render
        this.meshHandle = meshHandle;
        this.openMeshData = openMeshData;
        this.pocketMeshData = pocketMeshData;
        this.materialID = materialID;

        // Tool
        this.toolTypeID = toolTypeID;
        this.toolTier = toolTier;

        // Stacking
        this.stackSize = stackSize;

        // Block
        this.blockID = blockID;

        // Actions
        this.actions = actions;
        this.pickUpAsName = pickUpAsName;
        this.plantsTreeName = plantsTreeName;

        // Coverage
        this.sowsCoveringName = sowsCoveringName;
        this.nurtureLevels = nurtureLevels;
    }

    // Accessible \\

    public String getItemName() {
        return itemName;
    }

    public String getLocalName() {
        return localName;
    }

    public int getItemID() {
        return itemID;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public ItemCategory getCategory() {
        return category;
    }

    public float getWeight() {
        return weight;
    }

    public boolean isTwoHanded() {
        return twoHanded;
    }

    public boolean isSolid() {
        return solid;
    }

    public EquipmentType getEquipmentType() {
        return equipmentType;
    }

    public float getStat(ItemStat itemStat) {
        return stats[itemStat.ordinal()];
    }

    public ItemShapeStruct getShape() {
        return shape;
    }

    public boolean isContainer() {
        return containerSpace != null;
    }

    public ContainerSpaceStruct getContainerSpace() {
        return containerSpace;
    }

    public MeshHandle getMeshHandle() {
        return meshHandle;
    }

    public boolean hasOpenMesh() {
        return openMeshData != null;
    }

    public MeshData getOpenMeshData() {
        return openMeshData;
    }

    public MeshData getPocketMeshData() {
        return pocketMeshData;
    }

    public int getMaterialID() {
        return materialID;
    }

    public short getToolTypeID() {
        return toolTypeID;
    }

    public boolean isTool() {
        return toolTypeID != EngineSetting.TOOL_NONE;
    }

    public int getToolTier() {
        return toolTier;
    }

    public int getStackSize() {
        return stackSize;
    }

    public boolean isStackable() {
        return stackSize > 1;
    }

    public short getBlockID() {
        return blockID;
    }

    public boolean isBlockPiece() {
        return blockID != EngineSetting.BLOCK_PIECE_NONE;
    }

    public ObjectArrayList<ItemActionStruct> getActions() {
        return actions;
    }

    public boolean isPickedUpAsOther() {
        return !pickUpAsName.equals(EngineSetting.ITEM_PICK_UP_AS_SELF);
    }

    public String getPickUpAsName() {
        return pickUpAsName;
    }

    public boolean isSeed() {
        return !plantsTreeName.equals(EngineSetting.ITEM_PLANTS_NONE);
    }

    public String getPlantsTreeName() {
        return plantsTreeName;
    }

    public boolean isSower() {
        return !sowsCoveringName.equals(EngineSetting.ITEM_SOWS_NONE);
    }

    public String getSowsCoveringName() {
        return sowsCoveringName;
    }

    public boolean isNurturer() {
        return nurtureLevels != EngineSetting.ITEM_NURTURES_NONE;
    }

    public int getNurtureLevels() {
        return nurtureLevels;
    }
}
