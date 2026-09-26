package application.bootstrap.itempipeline.itemdefinition;

import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import engine.root.DataPackage;
import engine.util.mathematics.vectors.Vector3Int;

public class ItemDefinitionData extends DataPackage {

    /*
     * Immutable item definition payload loaded from JSON. Holds identity,
     * presentation, physical properties, the body slot it is worn in, the
     * statistics it grants, the sub-voxel shape it takes up in a container,
     * and render references for one item type. An item with a container size
     * holds its own container of that many sub-voxels. Owned by
     * ItemDefinitionHandle for the engine lifetime.
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

    // Equipment
    private final EquipmentType equipmentType;
    private final float[] stats;

    // Storage
    private final ItemShapeStruct shape;
    private final Vector3Int containerSize;

    // Render
    private final MeshHandle meshHandle;
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
            EquipmentType equipmentType,
            float[] stats,
            ItemShapeStruct shape,
            Vector3Int containerSize,
            MeshHandle meshHandle,
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

        // Equipment
        this.equipmentType = equipmentType;
        this.stats = stats;

        // Storage
        this.shape = shape;
        this.containerSize = containerSize;

        // Render
        this.meshHandle = meshHandle;
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
        return containerSize != null;
    }

    public Vector3Int getContainerSize() {
        return containerSize;
    }

    public MeshHandle getMeshHandle() {
        return meshHandle;
    }

    public int getMaterialID() {
        return materialID;
    }
}
