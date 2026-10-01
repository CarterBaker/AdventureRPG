package application.bootstrap.itempipeline.itemdefinition;

import application.bootstrap.geometrypipeline.mesh.MeshData;
import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import engine.root.DataPackage;

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
     * the inventory's menus. Owned by ItemDefinitionHandle for the engine
     * lifetime.
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
            int materialID) {

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
}
