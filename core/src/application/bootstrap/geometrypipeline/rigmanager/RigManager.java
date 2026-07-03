package application.bootstrap.geometrypipeline.rigmanager;

import application.bootstrap.geometrypipeline.rig.RigHandle;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;

public class RigManager extends ManagerPackage {

    /*
     * Owns the rig palette for the engine lifetime. A rig is a bone
     * hierarchy template shared by every entity that uses it — the
     * runtime pose lives on each entity's AnimationStateHandle, never
     * here. Auto-triggers an on-demand load via InternalLoader on a
     * name-based cache miss.
     */

    // Palette
    private Object2ObjectOpenHashMap<String, RigHandle> rigName2RigHandle;
    private Short2ObjectOpenHashMap<RigHandle> rigID2RigHandle;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.rigName2RigHandle = new Object2ObjectOpenHashMap<>();
        this.rigID2RigHandle = new Short2ObjectOpenHashMap<>();
        create(InternalLoader.class);
    }

    // Management \\

    void addRig(String rigName, RigHandle handle) {

        short id = RegistryUtility.toShortID(rigName);

        rigName2RigHandle.put(rigName, handle);
        rigID2RigHandle.put(id, handle);
    }

    // Accessible \\

    public boolean hasRig(String rigName) {
        return rigName2RigHandle.containsKey(rigName);
    }

    public RigHandle getRigHandleFromRigID(short rigID) {
        return rigID2RigHandle.get(rigID);
    }

    public RigHandle getRigHandleFromRigName(String rigName) {

        RigHandle handle = rigName2RigHandle.get(rigName);

        if (handle == null) {
            ((InternalLoader) internalLoader).request(rigName);
            handle = rigName2RigHandle.get(rigName);
        }

        if (handle == null)
            throwException("Rig could not be loaded: \"" + rigName + "\"");

        return handle;
    }
}