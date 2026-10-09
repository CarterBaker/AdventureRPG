package application.bootstrap.worldpipeline.worldmanager;

import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WorldManager extends ManagerPackage {

    /*
     * Owns the world palette and drives world loading via WorldLoader.
     * Tracks the active world and exposes the standard registry API. The active
     * world defaults to the starting world on first access if not yet set.
     */

    // Palette
    private Object2IntOpenHashMap<String> worldName2WorldID;
    private ObjectArrayList<WorldHandle> worldID2WorldHandle;

    // Active
    private WorldHandle activeWorld;

    // Internal \\

    @Override
    protected void create() {

        // Palette
        this.worldName2WorldID = RegistryUtility.createNameIndex();
        this.worldID2WorldHandle = RegistryUtility.createPalette();

        create(WorldLoader.class);
    }

    // Management \\

    int registerWorldName(String worldName) {
        return RegistryUtility.registerID(
                worldName2WorldID, worldID2WorldHandle, worldName, EngineSetting.REGISTRY_INT_ID_COUNT);
    }

    void addWorld(WorldHandle worldHandle) {

        worldID2WorldHandle.set(worldHandle.getWorldID(), worldHandle);

        if (activeWorld == null && worldHandle.getWorldName().equals(EngineSetting.STARTING_WORLD))
            activeWorld = worldHandle;
    }

    // Accessible \\

    public boolean hasWorld(String worldName) {
        return RegistryUtility.getHandle(worldName2WorldID, worldID2WorldHandle, worldName) != null;
    }

    public int getWorldIDFromWorldName(String worldName) {

        if (!hasWorld(worldName))
            request(worldName);

        if (!hasWorld(worldName))
            throwException("World not found after load: \"" + worldName + "\"");

        return worldName2WorldID.getInt(worldName);
    }

    public WorldHandle getWorldHandleFromWorldID(int worldID) {

        WorldHandle handle = RegistryUtility.getHandle(worldID2WorldHandle, worldID);

        if (handle == null)
            throwException("World ID not found: " + worldID);

        return handle;
    }

    public WorldHandle getWorldHandleFromWorldName(String worldName) {
        return getWorldHandleFromWorldID(getWorldIDFromWorldName(worldName));
    }

    public WorldHandle getActiveWorld() {

        if (activeWorld == null)
            activeWorld = getWorldHandleFromWorldName(EngineSetting.STARTING_WORLD);

        return activeWorld;
    }

    public void setActiveWorld(String worldName) {
        this.activeWorld = getWorldHandleFromWorldName(worldName);
    }

    public void request(String worldName) {
        ((WorldLoader) internalLoader).request(worldName);
    }
}