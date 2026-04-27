package application.bootstrap.shaderpipeline.passmanager;

import application.bootstrap.geometrypipeline.model.ModelInstance;
import application.bootstrap.shaderpipeline.material.MaterialInstance;
import application.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import application.bootstrap.shaderpipeline.pass.PassData;
import application.bootstrap.shaderpipeline.pass.PassHandle;
import application.bootstrap.shaderpipeline.pass.PassInstance;
import engine.root.ManagerPackage;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class PassManager extends ManagerPackage {

    private MaterialManager materialManager;

    private Object2IntOpenHashMap<String> passName2PassID;
    private Int2ObjectOpenHashMap<PassHandle> passID2PassHandle;

    @Override
    protected void create() {

        this.passName2PassID = new Object2IntOpenHashMap<>();
        this.passID2PassHandle = new Int2ObjectOpenHashMap<>();

        create(InternalLoader.class);
    }

    @Override
    protected void get() {
        this.materialManager = get(MaterialManager.class);
    }

    void addPassHandle(PassHandle handle) {
        passName2PassID.put(handle.getPassName(), handle.getPassID());
        passID2PassHandle.put(handle.getPassID(), handle);
    }

    public void request(String passName) {
        ((InternalLoader) internalLoader).request(passName);
    }

    public boolean hasPass(String passName) {
        return passName2PassID.containsKey(passName);
    }

    public int getPassIDFromPassName(String passName) {

        if (!passName2PassID.containsKey(passName))
            request(passName);

        return passName2PassID.getInt(passName);
    }

    public PassHandle getPassHandleFromPassID(int passID) {

        PassHandle handle = passID2PassHandle.get(passID);

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
