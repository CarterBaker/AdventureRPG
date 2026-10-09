package application.bootstrap.shaderpipeline.passmanager;

import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.bootstrap.shaderpipeline.pass.PassData;
import application.bootstrap.shaderpipeline.pass.PassHandle;
import application.bootstrap.shaderpipeline.pass.PassInstance;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class PassManager extends ManagerPackage {

    /*
     * Owns the full-screen pass palette, with IDs assigned in registration
     * order. Each pass pairs a quad model with its material, loaded on demand
     * on a miss.
     */

    private MaterialManager materialManager;

    private Object2IntOpenHashMap<String> passName2PassID;
    private ObjectArrayList<PassHandle> passID2PassHandle;

    @Override
    protected void create() {

        this.passName2PassID = RegistryUtility.createNameIndex();
        this.passID2PassHandle = RegistryUtility.createPalette();

        create(PassLoader.class);
    }

    @Override
    protected void get() {
        this.materialManager = get(MaterialManager.class);
    }

    int registerPassName(String passName) {
        return RegistryUtility.registerID(
                passName2PassID, passID2PassHandle, passName, EngineSetting.REGISTRY_INT_ID_COUNT);
    }

    void addPassHandle(PassHandle handle) {
        passID2PassHandle.set(handle.getPassID(), handle);
    }

    public void request(String passName) {
        ((PassLoader) internalLoader).request(passName);
    }

    public boolean hasPass(String passName) {
        return RegistryUtility.getHandle(passName2PassID, passID2PassHandle, passName) != null;
    }

    public int getPassIDFromPassName(String passName) {

        if (!hasPass(passName))
            request(passName);

        return passName2PassID.getInt(passName);
    }

    public PassHandle getPassHandleFromPassID(int passID) {

        PassHandle handle = RegistryUtility.getHandle(passID2PassHandle, passID);

        if (handle == null)
            throwException("Pass ID not found: " + passID);

        return handle;
    }

    public PassHandle getPassHandleFromPassName(String passName) {
        return getPassHandleFromPassID(getPassIDFromPassName(passName));
    }

    public PassInstance clonePass(int passID) {

        PassHandle handle = getPassHandleFromPassID(passID);
        MaterialInstance clonedMaterial = materialManager.cloneMaterial(
                handle.getMaterial().getMaterialID());

        ModelInstance modelInstance = create(ModelInstance.class);
        modelInstance.constructor(handle.getMeshHandle().getMeshData(), clonedMaterial);

        PassData clonedData = new PassData(
                handle.getPassName(),
                handle.getPassID(),
                handle.getMeshHandle(),
                clonedMaterial,
                modelInstance);

        PassInstance instance = create(PassInstance.class);
        instance.constructor(clonedData);

        return instance;
    }

    public PassInstance clonePass(String passName) {
        return clonePass(getPassIDFromPassName(passName));
    }
}
