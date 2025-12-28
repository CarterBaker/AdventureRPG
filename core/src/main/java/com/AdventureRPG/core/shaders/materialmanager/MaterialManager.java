package com.AdventureRPG.core.shaders.materialmanager;

import com.AdventureRPG.core.engine.ManagerFrame;
import com.AdventureRPG.core.shaders.materials.Material;
import com.AdventureRPG.core.shaders.shadermanager.ShaderManager;
import com.AdventureRPG.core.shaders.shaders.Shader;
import com.AdventureRPG.core.shaders.ubomanager.UBOHandle;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class MaterialManager extends ManagerFrame {

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
        this.internalLoadManager = (InternalLoadManager) register(new InternalLoadManager());

        // Retrieval Mapping
        this.materialName2MaterialID = new Object2IntOpenHashMap<>();
        this.materialID2Material = new Int2ObjectOpenHashMap<>();
    }

    @Override
    protected void init() {

        // Internal
        this.shaderManager = gameEngine.get(ShaderManager.class);
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
