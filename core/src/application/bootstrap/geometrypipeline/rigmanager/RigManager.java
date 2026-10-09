package application.bootstrap.geometrypipeline.rigmanager;

import application.bootstrap.geometrypipeline.rig.RigHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RigManager extends ManagerPackage {

    /*
     * Owns the rig palette for the engine lifetime. A rig is a bone
     * hierarchy template shared by every entity that uses it — the
     * runtime pose lives on each entity's AnimationStateHandle, never
     * here. Rig IDs are assigned in registration order. Auto-triggers an
     * on-demand load via RigLoader on a name-based cache miss.
     */

    // Palette
    private Object2IntOpenHashMap<String> rigName2RigID;
    private ObjectArrayList<RigHandle> rigID2RigHandle;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.rigName2RigID = RegistryUtility.createNameIndex();
        this.rigID2RigHandle = RegistryUtility.createPalette();
        create(RigLoader.class);
    }

    // Management \\

    void addRig(String rigName, RigHandle handle) {
        RegistryUtility.registerHandle(
                rigName2RigID, rigID2RigHandle, rigName, handle, EngineSetting.REGISTRY_SHORT_ID_COUNT);
    }

    // Accessible \\

    public boolean hasRig(String rigName) {
        return RegistryUtility.getHandle(rigName2RigID, rigID2RigHandle, rigName) != null;
    }

    public short getRigIDFromRigName(String rigName) {

        if (!hasRig(rigName))
            ((RigLoader) internalLoader).request(rigName);

        if (!hasRig(rigName))
            throwException("Rig could not be loaded: \"" + rigName + "\"");

        return (short) rigName2RigID.getInt(rigName);
    }

    public RigHandle getRigHandleFromRigID(short rigID) {
        return RegistryUtility.getHandle(rigID2RigHandle, rigID);
    }

    public RigHandle getRigHandleFromRigName(String rigName) {
        return getRigHandleFromRigID(getRigIDFromRigName(rigName));
    }
}
