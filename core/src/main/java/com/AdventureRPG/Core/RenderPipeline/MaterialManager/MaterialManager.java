package com.AdventureRPG.Core.RenderPipeline.MaterialManager;

import com.AdventureRPG.Core.Bootstrap.ManagerFrame;
import com.AdventureRPG.Core.RenderPipeline.Material.Material;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class MaterialManager extends ManagerFrame {

    // Internal
    private InternalLoadManager internalLoadManager;

    // Retrieval Mapping
    private Object2IntOpenHashMap<String> materialName2MaterialID;
    private Int2ObjectOpenHashMap<Material> materialID2Material;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.internalLoadManager = (InternalLoadManager) register(new InternalLoadManager());

        // Retrieval Mapping
        this.materialName2MaterialID = new Object2IntOpenHashMap<>();
        this.materialID2Material = new Int2ObjectOpenHashMap<>();
    }

    @Override
    protected void awake() {
        compileMaterials();
    }

    @Override
    protected void freeMemory() {
        internalLoadManager = (InternalLoadManager) release(internalLoadManager);
    }

    // Material Management \\

    private void compileMaterials() {
        internalLoadManager.loadMaterials();
    }

    void addMaterial(Material material) {
        materialName2MaterialID.put(material.materialName, material.materialID);
        materialID2Material.put(material.materialID, material);
    }

    // Accessible \\

    public int getMaterialIDFromMaterialName(String materialName) {
        return materialName2MaterialID.getInt(materialName);
    }

    public Material getMaterialFromMaterialID(int materialID) {
        return materialID2Material.get(materialID);
    }
}
