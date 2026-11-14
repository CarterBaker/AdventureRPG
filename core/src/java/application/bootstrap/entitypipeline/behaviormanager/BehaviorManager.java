package application.bootstrap.entitypipeline.behaviormanager;

import application.bootstrap.entitypipeline.behavior.BehaviorHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class BehaviorManager extends ManagerPackage {

    /*
     * Owns the behavior palette for the engine lifetime. Supports lookup by
     * both name and short ID, with IDs assigned in registration order.
     * Auto-triggers an on-demand load via BehaviorLoader on a name-based
     * cache miss.
     */

    // Palette
    private Object2IntOpenHashMap<String> behaviorName2BehaviorID;
    private ObjectArrayList<BehaviorHandle> behaviorID2BehaviorHandle;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.behaviorName2BehaviorID = RegistryUtility.createNameIndex();
        this.behaviorID2BehaviorHandle = RegistryUtility.createPalette();
        create(BehaviorLoader.class);
    }

    // Management \\

    short registerBehaviorName(String behaviorName) {
        return (short) RegistryUtility.registerID(
                behaviorName2BehaviorID, behaviorID2BehaviorHandle, behaviorName,
                EngineSetting.REGISTRY_SHORT_ID_COUNT);
    }

    void addBehavior(BehaviorHandle handle) {
        behaviorID2BehaviorHandle.set(handle.getBehaviorID(), handle);
    }

    // Accessible \\

    public boolean hasBehavior(String behaviorName) {
        return RegistryUtility.getHandle(behaviorName2BehaviorID, behaviorID2BehaviorHandle, behaviorName) != null;
    }

    public short getBehaviorIDFromBehaviorName(String behaviorName) {

        if (!hasBehavior(behaviorName))
            ((BehaviorLoader) internalLoader).request(behaviorName);

        if (!hasBehavior(behaviorName))
            throwException("[BehaviorManager] Behavior could not be loaded: \"" + behaviorName + "\"");

        return (short) behaviorName2BehaviorID.getInt(behaviorName);
    }

    public BehaviorHandle getBehaviorHandleFromBehaviorID(short behaviorID) {
        return RegistryUtility.getHandle(behaviorID2BehaviorHandle, behaviorID);
    }

    public BehaviorHandle getBehaviorHandleFromBehaviorName(String behaviorName) {
        return getBehaviorHandleFromBehaviorID(getBehaviorIDFromBehaviorName(behaviorName));
    }
}
