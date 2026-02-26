package com.internal.bootstrap.shaderpipeline.materialmanager;

import com.internal.bootstrap.shaderpipeline.shadermanager.ShaderManager;
import com.internal.bootstrap.shaderpipeline.ubomanager.UBOHandle;
import com.internal.bootstrap.shaderpipeline.uniforms.Uniform;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;
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

        // UBOs are shared — clones reference the same GPU buffers intentionally
        Object2ObjectOpenHashMap<String, UBOHandle> clonedBuffers = new Object2ObjectOpenHashMap<>(
                originalMaterial.getUBOs());

        // Deep copy uniforms — each clone gets its own Uniform instances so
        // mutations on one clone do not affect the base material or other clones
        Object2ObjectOpenHashMap<String, Uniform<?>> deepCopiedUniforms = new Object2ObjectOpenHashMap<>();
        for (var entry : originalMaterial.getUniforms().object2ObjectEntrySet()) {
            Uniform<?> original = entry.getValue();
            UniformAttribute<?> freshAttr = original.attribute.createDefault();
            Uniform<?> freshUniform = new Uniform<>(original.uniformHandle, original.offset, freshAttr);
            deepCopiedUniforms.put(entry.getKey(), freshUniform);
        }

        // Create new material handle with deep copied uniforms
        MaterialHandle clonedMaterial = create(MaterialHandle.class);
        clonedMaterial.constructor(
                originalMaterial.getMaterialName(),
                originalMaterial.getMaterialID(),
                originalMaterial.getShaderHandle(),
                clonedBuffers,
                deepCopiedUniforms);

        return clonedMaterial;
    }
}
