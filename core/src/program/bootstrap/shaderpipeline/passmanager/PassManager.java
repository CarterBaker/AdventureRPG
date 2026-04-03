package program.bootstrap.shaderpipeline.passmanager;

import program.bootstrap.geometrypipeline.model.ModelInstance;
import program.bootstrap.renderpipeline.rendermanager.RenderManager;
import program.bootstrap.shaderpipeline.material.MaterialInstance;
import program.bootstrap.shaderpipeline.materialmanager.MaterialManager;
import program.bootstrap.shaderpipeline.pass.PassData;
import program.bootstrap.shaderpipeline.pass.PassHandle;
import program.bootstrap.shaderpipeline.pass.PassInstance;
import program.core.engine.ManagerPackage;
import program.core.kernel.window.WindowInstance;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class PassManager extends ManagerPackage {

    /*
     * Owns all compiled PassHandles. Drives bootstrap via InternalLoader.
     * Passes are pushed into the render system by depth layer. Cloning produces
     * a PassInstance with a shared mesh reference and a deep-copied
     * MaterialInstance,
     * mirroring the material cloning contract.
     */

    // Internal
    private MaterialManager materialManager;
    private RenderManager renderSystem;

    // Palette
    private Object2IntOpenHashMap<String> passName2PassID;
    private Int2ObjectOpenHashMap<PassHandle> passID2PassHandle;

    // Base \\

    @Override
    protected void create() {

        this.passName2PassID = new Object2IntOpenHashMap<>();
        this.passID2PassHandle = new Int2ObjectOpenHashMap<>();

        create(InternalLoader.class);
    }

    @Override
    protected void get() {
        this.materialManager = get(MaterialManager.class);
        this.renderSystem = get(RenderManager.class);
    }

    // Management \\

    void addPassHandle(PassHandle handle) {
        passName2PassID.put(handle.getPassName(), handle.getPassID());
        passID2PassHandle.put(handle.getPassID(), handle);
    }

    // Render \\

    // Push — no declared window → main window \\

    public void pushPass(PassHandle pass, int depth) {
        renderSystem.pushRenderCall(pass.getModelInstance(), depth);
    }

    public void pushPass(PassInstance pass, int depth) {
        renderSystem.pushRenderCall(pass.getModelInstance(), depth);
    }

    // Push — explicit window \\

    public void pushPass(PassHandle pass, int depth, WindowInstance window) {
        renderSystem.pushRenderCall(pass.getModelInstance(), depth, window);
    }

    public void pushPass(PassInstance pass, int depth, WindowInstance window) {
        renderSystem.pushRenderCall(pass.getModelInstance(), depth, window);
    }

    // Accessible \\

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