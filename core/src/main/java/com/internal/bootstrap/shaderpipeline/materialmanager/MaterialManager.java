package com.internal.bootstrap.shaderpipeline.materialmanager;

import com.internal.bootstrap.shaderpipeline.shadermanager.ShaderManager;
import com.internal.bootstrap.shaderpipeline.ubomanager.UBOHandle;
import com.internal.core.engine.ManagerPackage;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class MaterialManager extends ManagerPackage {

    // Internal
    private ShaderManager shaderManager;
    private InternalLoadManager internalLoadManager;

    // Retrieval Mapping
    private Object2IntOpenHashMap<String> materialName2MaterialID;
    private Int2ObjectOpenHashMap<MaterialHandle> materialID2Material;

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

    void addMaterial(MaterialHandle material) {
        materialName2MaterialID.put(material.getMaterialName(), material.getMaterialID());
        materialID2Material.put(material.getMaterialID(), material);
    }

    // Accessible \\

    public void bindShaderToUBO(
            MaterialHandle material,
            UBOHandle ubo) {
        shaderManager.bindShaderToUBO(
                material.getShaderHandle(),
                ubo);
    }

    public int getMaterialIDFromMaterialName(String materialName) {
        return materialName2MaterialID.getInt(materialName);
    }

    public MaterialHandle getMaterialFromMaterialID(int materialID) {
        return materialID2Material.get(materialID);
    }
}
