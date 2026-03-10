package com.internal.bootstrap.itempipeline.itemdefinition;

import com.internal.bootstrap.geometrypipeline.mesh.MeshHandle;
import com.internal.core.engine.HandlePackage;

public class ItemDefinitionHandle extends HandlePackage {

    // Identity
    private String itemName;
    private int itemID;

    // Properties
    private float weight;
    private boolean twoHanded;
    private boolean isBackpack;

    // Render
    private MeshHandle meshHandle;
    private int materialID;

    // Constructor \\

    public void constructor(String itemName, int itemID, float weight,
            boolean twoHanded, boolean isBackpack,
            MeshHandle meshHandle, int materialID) {
        this.itemName = itemName;
        this.itemID = itemID;
        this.weight = weight;
        this.twoHanded = twoHanded;
        this.isBackpack = isBackpack;
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

    public short getNameShort() {
        return (short) ((itemID >> 16) & 0xFFFF);
    }

    public short getEnchantShort() {
        return (short) (itemID & 0xFFFF);
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