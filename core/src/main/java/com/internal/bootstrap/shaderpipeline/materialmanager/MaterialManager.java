package com.internal.bootstrap.shaderpipeline.materialmanager;

import com.internal.bootstrap.shaderpipeline.material.MaterialInstance;
import com.internal.bootstrap.shaderpipeline.shadermanager.ShaderManager;
import com.internal.bootstrap.shaderpipeline.ubomanager.UBOHandle;
import com.internal.bootstrap.shaderpipeline.uniforms.Uniform;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.internal.core.engine.ManagerPackage;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

/*
 * Owns all compiled MaterialHandle objects for the lifetime of the application.
 * Delegates bootstrap compilation to InternalLoadManager, which is released once
 * all materials are assembled. Cloning produces a MaterialInstance with a back-reference
 * to the source handle, shared UBO state, and a deep-copied uniform map.
 */
public class MaterialManager extends ManagerPackage {

    // Internal
    private ShaderManager shaderManager;
    private InternalLoadManager internalLoadManager;

    // Retrieval Mapping
    private Object2IntOpenHashMap<String> materialName2MaterialID;
    private Int2ObjectOpenHashMap<MaterialHandle> materialID2Material;

    // Internal \\

    @Override
    protected void create() {
        this.internalLoadManager = create(InternalLoadManager.class);
        this.materialName2MaterialID = new Object2IntOpenHashMap<>();
        this.materialID2Material = new Int2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {
        this.shaderManager = get(ShaderManager.class);
    }

    @Override
    protected void awake() {
        internalLoadManager.loadMaterials();
    }

    @Override
    protected void release() {
        this.internalLoadManager = release(InternalLoadManager.class);
    }

    // Material Management \\

    void addMaterial(MaterialHandle material) {
        materialName2MaterialID.put(material.getMaterialName(), material.getMaterialID());
        materialID2Material.put(material.getMaterialID(), material);
    }

    // Accessible \\

    public void bindShaderToUBO(MaterialHandle material, UBOHandle ubo) {
        shaderManager.bindShaderToUBO(material.getShaderHandle(), ubo.getBufferName());
    }

    public int getMaterialIDFromMaterialName(String materialName) {
        return materialName2MaterialID.getInt(materialName);
    }

    public MaterialHandle getMaterialFromMaterialID(int materialID) {
        return materialID2Material.get(materialID);
    }

    public MaterialInstance cloneMaterial(int materialID) {

        MaterialHandle original = getMaterialFromMaterialID(materialID);

        if (original == null)
            throwException("Cannot clone material — materialID " + materialID + " not found");

        Object2ObjectOpenHashMap<String, Uniform<?>> sourceUniforms = original.getUniforms();
        Object2ObjectOpenHashMap<String, Uniform<?>> deepCopiedUniforms = new Object2ObjectOpenHashMap<>();

        String[] keys = sourceUniforms.keySet().toArray(new String[0]);

        for (int i = 0; i < keys.length; i++) {
            Uniform<?> source = sourceUniforms.get(keys[i]);
            UniformAttribute<?> freshAttr = source.attribute().createDefault();
            deepCopiedUniforms.put(keys[i], new Uniform<>(source.uniformHandle, source.offset, freshAttr));
        }

        MaterialInstance instance = create(MaterialInstance.class);
        instance.constructor(original, deepCopiedUniforms);

        return instance;
    }
}