package com.internal.bootstrap.shaderpipeline.materialmanager;

import com.internal.bootstrap.shaderpipeline.shadermanager.ShaderManager;
import com.internal.bootstrap.shaderpipeline.ubomanager.UBOHandle;
import com.internal.bootstrap.shaderpipeline.uniforms.Uniform;
import com.internal.core.engine.ManagerPackage;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

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

    public MaterialHandle cloneMaterial(int materialID) {

        MaterialHandle originalMaterial = getMaterialFromMaterialID(materialID);

        if (originalMaterial == null)
            throwException("Cannot clone material - materialID " + materialID + " not found");

        // Clone UBOs map (shallow copy - UBOHandles themselves are shared)
        Object2ObjectOpenHashMap<String, UBOHandle> clonedBuffers = new Object2ObjectOpenHashMap<>(
                originalMaterial.getUBOs());

        // Clone uniforms map (shallow copy - Uniform objects themselves are shared)
        Object2ObjectOpenHashMap<String, Uniform<?>> clonedUniforms = new Object2ObjectOpenHashMap<>(
                originalMaterial.getUniforms());

        // Create new material handle with cloned maps
        MaterialHandle clonedMaterial = create(MaterialHandle.class);
        clonedMaterial.constructor(
                originalMaterial.getMaterialName(),
                originalMaterial.getMaterialID(),
                originalMaterial.getShaderHandle(),
                clonedBuffers,
                clonedUniforms);

        return clonedMaterial;
    }
}
