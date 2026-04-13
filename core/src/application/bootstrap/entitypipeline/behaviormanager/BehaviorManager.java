package application.bootstrap.entitypipeline.behaviormanager;

import application.bootstrap.entitypipeline.behavior.BehaviorHandle;
import application.core.engine.ManagerPackage;
import application.core.util.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;

public class BehaviorManager extends ManagerPackage {

    /*
     * Owns the behavior palette for the engine lifetime. Supports lookup by
     * both name and short ID. Auto-triggers an on-demand load via
     * InternalLoader on a name-based cache miss.
     */

    // Palette
    private Object2ObjectOpenHashMap<String, BehaviorHandle> behaviorName2BehaviorHandle;
    private Short2ObjectOpenHashMap<BehaviorHandle> behaviorID2BehaviorHandle;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.behaviorName2BehaviorHandle = new Object2ObjectOpenHashMap<>();
        this.behaviorID2BehaviorHandle = new Short2ObjectOpenHashMap<>();
        create(InternalLoader.class);
    }

    // Management \\

    void addBehavior(BehaviorHandle handle) {

        behaviorName2BehaviorHandle.put(handle.getBehaviorName(), handle);
        behaviorID2BehaviorHandle.put(handle.getBehaviorID(), handle);
    }

    // Accessible \\

    public boolean hasBehavior(String behaviorName) {
        return behaviorName2BehaviorHandle.containsKey(behaviorName);
    }

    public short getBehaviorIDFromBehaviorName(String behaviorName) {

        if (!behaviorName2BehaviorHandle.containsKey(behaviorName))
            ((InternalLoader) internalLoader).request(behaviorName);

        return RegistryUtility.toShortID(behaviorName);
    }

    public BehaviorHandle getBehaviorHandleFromBehaviorID(short behaviorID) {
        return behaviorID2BehaviorHandle.get(behaviorID);
    }

    public BehaviorHandle getBehaviorHandleFromBehaviorName(String behaviorName) {

        BehaviorHandle handle = behaviorName2BehaviorHandle.get(behaviorName);

        if (handle == null) {
            ((InternalLoader) internalLoader).request(behaviorName);
            handle = behaviorName2BehaviorHandle.get(behaviorName);
        }

        if (handle == null)
            throwException("[BehaviorManager] Behavior could not be loaded: \"" + behaviorName + "\"");

        return handle;
    }
}