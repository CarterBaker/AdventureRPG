package application.bootstrap.shaderpipeline.materialmanager;

import application.bootstrap.shaderpipeline.material.MaterialData;
import application.bootstrap.shaderpipeline.material.MaterialHandle;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.core.engine.ManagerPackage;
import application.core.util.RegistryUtility;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class MaterialManager extends ManagerPackage {

    /*
     * Owns all material handles. Drives loading via InternalLoader and exposes
     * cloneMaterial() for runtime instance creation. Handles are persistent —
     * instances are cloned on demand and discarded by the caller.
     */

    // Palette
    private Object2IntOpenHashMap<String> materialName2MaterialID;
    private Int2ObjectOpenHashMap<MaterialHandle> materialID2MaterialHandle;

    // Base \\

    @Override
    protected void create() {

        this.materialName2MaterialID = new Object2IntOpenHashMap<>();
        this.materialID2MaterialHandle = new Int2ObjectOpenHashMap<>();
        this.materialName2MaterialID.defaultReturnValue(-1);

        create(InternalLoader.class);
    }

    // Management \\

    void addMaterial(String materialName, MaterialHandle handle) {
        int id = RegistryUtility.toIntID(materialName);
        materialName2MaterialID.put(materialName, id);
        materialID2MaterialHandle.put(id, handle);
    }

    // On-Demand \\

    public void request(String materialName) {
        ((InternalLoader) internalLoader).request(materialName);
    }

    // Accessible \\

    public boolean hasMaterial(String materialName) {
        return materialName2MaterialID.containsKey(materialName);
    }

    public int getMaterialIDFromMaterialName(String materialName) {

        if (!materialName2MaterialID.containsKey(materialName))
            request(materialName);

        if (!materialName2MaterialID.containsKey(materialName))
            throwException("Material not found after load: '" + materialName + "'");

        return materialName2MaterialID.getInt(materialName);
    }

    public MaterialHandle getMaterialHandleFromMaterialID(int materialID) {

        MaterialHandle handle = materialID2MaterialHandle.get(materialID);

        if (handle == null)
            throwException("No handle registered for material ID: " + materialID);

        return handle;
    }

    public MaterialHandle getMaterialHandleFromMaterialName(String materialName) {
        return getMaterialHandleFromMaterialID(getMaterialIDFromMaterialName(materialName));
    }

    public MaterialInstance cloneMaterial(String materialName) {
        MaterialHandle handle = getMaterialHandleFromMaterialName(materialName);
        MaterialData clonedData = new MaterialData(handle.getMaterialData());
        MaterialInstance instance = create(MaterialInstance.class);
        instance.constructor(clonedData);
        return instance;
    }

    public MaterialInstance cloneMaterial(int materialID) {
        MaterialHandle handle = getMaterialHandleFromMaterialID(materialID);
        MaterialData clonedData = new MaterialData(handle.getMaterialData());
        MaterialInstance instance = create(MaterialInstance.class);
        instance.constructor(clonedData);
        return instance;
    }
}