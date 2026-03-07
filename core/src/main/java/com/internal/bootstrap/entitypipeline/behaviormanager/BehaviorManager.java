package com.internal.bootstrap.entitypipeline.behaviormanager;

import com.internal.bootstrap.entitypipeline.behavior.BehaviorHandle;
import com.internal.core.engine.ManagerPackage;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;

public class BehaviorManager extends ManagerPackage {

    // Palette
    private Object2ObjectOpenHashMap<String, BehaviorHandle> name2Behavior;
    private Short2ObjectOpenHashMap<BehaviorHandle> id2Behavior;

    // Base \\

    @Override
    protected void create() {
        this.name2Behavior = new Object2ObjectOpenHashMap<>();
        this.id2Behavior = new Short2ObjectOpenHashMap<>();
        create(InternalLoader.class);
    }

    // Management \\

    void addBehavior(BehaviorHandle handle) {
        name2Behavior.put(handle.getBehaviorName(), handle);
        id2Behavior.put(handle.getBehaviorID(), handle);
    }

    // Accessible \\

    public BehaviorHandle getBehavior(String behaviorName) {

        BehaviorHandle handle = name2Behavior.get(behaviorName);

        if (handle == null) {
            ((InternalLoader) internalLoader).request(behaviorName);
            handle = name2Behavior.get(behaviorName);
        }

        if (handle == null)
            throwException("[BehaviorManager] Behavior could not be loaded: \"" + behaviorName + "\"");

        return handle;
    }

    public BehaviorHandle getBehavior(short behaviorID) {
        return id2Behavior.get(behaviorID);
    }
}