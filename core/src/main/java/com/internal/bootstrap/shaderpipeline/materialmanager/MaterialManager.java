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
 * Delegates bootstrap compilation to InternalLoadManager, which self-releases
 * once all materials are assembled. On accessor miss, triggers an immediate
 * synchronous load through the active InternalLoadManager.
 */
public class MaterialManager extends ManagerPackage {

    // Internal
    private ShaderManager shaderManager;

    // Retrieval Mapping
    private Object2IntOpenHashMap<String> materialName2MaterialID;
    private Int2ObjectOpenHashMap<MaterialHandle> materialID2Material;

    // Base \\

    @Override
    protected void create() {
        create(InternalLoadManager.class);
        this.materialName2MaterialID = new Object2IntOpenHashMap<>();
        this.materialID2Material = new Int2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {
        this.shaderManager = get(ShaderManager.class);
    }

    // On-Demand Loading \\

    public void request(String materialName) {
        ((InternalLoadManager) internalLoader).request(materialName);
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
        if (!materialName2MaterialID.containsKey(materialName))
            request(materialName);
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
            freshAttr.setObject(source.attribute().getValue());
            deepCopiedUniforms.put(keys[i], new Uniform<>(source.uniformHandle, source.offset, freshAttr));
        }

        MaterialInstance instance = create(MaterialInstance.class);
        instance.constructor(
                original.getMaterialName(),
                original.getMaterialID(),
                original.getShaderHandle(),
                original.getUBOs(),
                deepCopiedUniforms);

        return instance;
    }
}