package com.AdventureRPG.core.shaderpipeline.materialmanager;

import com.AdventureRPG.core.engine.ManagerPackage;
import com.AdventureRPG.core.shaderpipeline.materials.Material;
import com.AdventureRPG.core.shaderpipeline.shadermanager.ShaderManager;
import com.AdventureRPG.core.shaderpipeline.ubomanager.UBOHandle;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class MaterialManager extends ManagerPackage {

    // Internal
    private ShaderManager shaderManager;
    private InternalLoadManager internalLoadManager;

    // Retrieval Mapping
    private Object2IntOpenHashMap<String> materialName2MaterialID;
    private Int2ObjectOpenHashMap<Material> materialID2Material;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.internalLoadManager = create(InternalLoadManager.class);

        // Retrieval Mapping
        this.materialName2MaterialID = new Object2IntOpenHashMap<>();
        this.materialID2Material = new Int2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {

        // Internal
        this.shaderManager = get(ShaderManager.class);
    }

    @Override
    protected void awake() {
        compileMaterials();
    }

    @Override
    protected void release() {
        internalLoadManager = release(InternalLoadManager.class);
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

    public void bindShaderToUBO(
            Material material,
            UBOHandle ubo) {
        shaderManager.bindShaderToUBO(
                material.shader,
                ubo);
    }

    public int getMaterialIDFromMaterialName(String materialName) {
        return materialName2MaterialID.getInt(materialName);
    }

    public Material getMaterialFromMaterialID(int materialID) {
        return materialID2Material.get(materialID);
    }
}
