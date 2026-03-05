package com.internal.bootstrap.worldpipeline.worldstreammanager;

import com.internal.core.engine.ManagerPackage;
import com.internal.core.engine.settings.EngineSetting;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class WorldStreamManager extends ManagerPackage {

    // Retrieval Mapping
    private Object2IntOpenHashMap<String> worldName2WorldID;
    private Int2ObjectOpenHashMap<WorldHandle> worldID2World;

    // Active World
    private WorldHandle activeWorld;

    // Base \\

    @Override
    protected void create() {
        create(InternalLoadManager.class);
        this.worldName2WorldID = new Object2IntOpenHashMap<>();
        this.worldID2World = new Int2ObjectOpenHashMap<>();
    }

    // On-Demand Loading \\

    public void request(String worldName) {
        ((InternalLoadManager) internalLoader).request(worldName);
    }

    // World Management \\

    void addWorld(WorldHandle world) {
        worldName2WorldID.put(world.getWorldName(), world.getWorldID());
        worldID2World.put(world.getWorldID(), world);
        if (activeWorld == null && world.getWorldName().equals(EngineSetting.STARTING_WORLD))
            activeWorld = world;
    }

    // Accessible \\

    public void setActiveWorld(int worldID) {
        this.activeWorld = worldID2World.get(worldID);
    }

    public WorldHandle getActiveWorld() {

        if (activeWorld == null) {
            int worldID = getWorldIDFromWorldName(EngineSetting.STARTING_WORLD);
            setActiveWorld(worldID);

            if (activeWorld == null)
                throwException("[WorldStreamManager] Starting world could not be loaded: \""
                        + EngineSetting.STARTING_WORLD + "\"");
        }

        return activeWorld;
    }

    public int getWorldIDFromWorldName(String worldName) {
        if (!worldName2WorldID.containsKey(worldName))
            request(worldName);
        return worldName2WorldID.getInt(worldName);
    }

    public WorldHandle getWorldFromWorldID(int worldID) {
        return worldID2World.get(worldID);
    }
}