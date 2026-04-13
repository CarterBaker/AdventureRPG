package application.bootstrap.itempipeline.itemdefinition;

import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.core.engine.DataPackage;

public class ItemDefinitionData extends DataPackage {

    /*
     * Immutable item definition payload loaded from JSON. Holds identity,
     * physical properties, and render references for one item type. Owned
     * by ItemDefinitionHandle for the engine lifetime.
     */

    // Identity
    private final String itemName;
    private final int itemID;

    // Properties
    private final float weight;
    private final boolean twoHanded;
    private final boolean isBackpack;

    // Render
    private final MeshHandle meshHandle;
    private final int materialID;

    // Constructor \\

    public ItemDefinitionData(
            String itemName,
            int itemID,
            float weight,
            boolean twoHanded,
            boolean isBackpack,
            MeshHandle meshHandle,
            int materialID) {

        // Identity
        this.itemName = itemName;
        this.itemID = itemID;

        // Properties
        this.weight = weight;
        this.twoHanded = twoHanded;
        this.isBackpack = isBackpack;

        // Render
        this.meshHandle = meshHandle;
        this.materialID = materialID;
    }

    // Accessible \\

    public String getItemName() {
        return itemName;
    }

    public int getItemID() {
        return itemID;
    }

    public float getWeight() {
        return weight;
    }

    public boolean isTwoHanded() {
        return twoHanded;
    }

    public boolean isBackpack() {
        return isBackpack;
    }

    public MeshHandle getMeshHandle() {
        return meshHandle;
    }

    public int getMaterialID() {
        return materialID;
    }
}