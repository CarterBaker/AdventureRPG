package com.AdventureRPG.bootstrap.worldpipeline.worldstreammanager;

import com.AdventureRPG.core.engine.ManagerPackage;
import com.AdventureRPG.core.engine.settings.EngineSetting;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class WorldStreamManager extends ManagerPackage {

    // Internal
    private InternalLoadManager internalLoadManager;

    // Retrieval Mapping
    private Object2IntOpenHashMap<String> worldName2WorldID;
    private Int2ObjectOpenHashMap<WorldHandle> worldID2World;

    // Active World
    private WorldHandle activeWorld;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.internalLoadManager = create(InternalLoadManager.class);

        // Retrieval Mapping
        this.worldName2WorldID = new Object2IntOpenHashMap<>();
        this.worldID2World = new Int2ObjectOpenHashMap<>();
    }

    @Override
    protected void awake() {

        compileWorlds();

        int worldID = getWorldIDFromWorldName(EngineSetting.STARTING_WORLD);
        activeWorld = getWorldFromWorldID(worldID);
    }

    @Override
    protected void release() {
        internalLoadManager = release(InternalLoadManager.class);
    }

    // World Management \\

    private void compileWorlds() {
        internalLoadManager.loadWorlds();
    }

    void addWorld(WorldHandle world) {
        worldName2WorldID.put(world.getWorldName(), world.getWorldID());
        worldID2World.put(world.getWorldID(), world);
    }

    // Accessible \\

    public void setActiveWorld(int worldID) {
        this.activeWorld = worldID2World.get(worldID);
    }

    public WorldHandle getActiveWorld() {
        return activeWorld;
    }

    public int getWorldIDFromWorldName(String worldName) {
        return worldName2WorldID.getInt(worldName);
    }

    public WorldHandle getWorldFromWorldID(int worldID) {
        return worldID2World.get(worldID);
    }
}