package application.bootstrap.shaderpipeline.materialmanager;

import application.bootstrap.shaderpipeline.material.MaterialData;
import application.bootstrap.shaderpipeline.material.MaterialHandle;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MaterialManager extends ManagerPackage {

    /*
     * Owns all material handles, with IDs assigned in registration order.
     * Drives loading via MaterialLoader and exposes cloneMaterial() for
     * runtime instance creation. Handles are persistent — instances are
     * cloned on demand and discarded by the caller.
     */

    // Palette
    private Object2IntOpenHashMap<String> materialName2MaterialID;
    private ObjectArrayList<MaterialHandle> materialID2MaterialHandle;

    // Base \\

    @Override
    protected void create() {

        this.materialName2MaterialID = RegistryUtility.createNameIndex();
        this.materialID2MaterialHandle = RegistryUtility.createPalette();

        create(MaterialLoader.class);
    }

    // Management \\

    int registerMaterialName(String materialName) {
        return RegistryUtility.registerID(
                materialName2MaterialID, materialID2MaterialHandle, materialName,
                EngineSetting.REGISTRY_INT_ID_COUNT);
    }

    void addMaterial(MaterialHandle handle) {
        materialID2MaterialHandle.set(handle.getMaterialID(), handle);
    }

    // On-Demand \\

    public void request(String materialName) {
        ((MaterialLoader) internalLoader).request(materialName);
    }

    // Accessible \\

    public boolean hasMaterial(String materialName) {
        return RegistryUtility.getHandle(materialName2MaterialID, materialID2MaterialHandle, materialName) != null;
    }

    public int getMaterialIDFromMaterialName(String materialName) {

        if (!hasMaterial(materialName))
            request(materialName);

        if (!hasMaterial(materialName))
            throwException("Material not found after load: '" + materialName + "'");

        return materialName2MaterialID.getInt(materialName);
    }

    public MaterialHandle getMaterialHandleFromMaterialID(int materialID) {

        MaterialHandle handle = RegistryUtility.getHandle(materialID2MaterialHandle, materialID);

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