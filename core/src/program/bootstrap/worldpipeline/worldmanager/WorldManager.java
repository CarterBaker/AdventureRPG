package program.bootstrap.worldpipeline.worldmanager;

import program.bootstrap.worldpipeline.world.WorldHandle;
import program.core.engine.ManagerPackage;
import program.core.settings.EngineSetting;
import program.core.util.RegistryUtility;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class WorldManager extends ManagerPackage {

    /*
     * Owns the world palette and drives world loading via InternalLoader.
     * Tracks the active world and exposes the standard registry API. The active
     * world defaults to the starting world on first access if not yet set.
     */

    // Palette
    private Object2IntOpenHashMap<String> worldName2WorldID;
    private Int2ObjectOpenHashMap<WorldHandle> worldID2WorldHandle;

    // Active
    private WorldHandle activeWorld;

    // Internal \\

    @Override
    protected void create() {

        // Palette
        this.worldName2WorldID = new Object2IntOpenHashMap<>();
        this.worldID2WorldHandle = new Int2ObjectOpenHashMap<>();
        this.worldName2WorldID.defaultReturnValue(-1);

        create(InternalLoader.class);
    }

    // Management \\

    void addWorld(String worldName, WorldHandle worldHandle) {

        int id = RegistryUtility.toIntID(worldName);
        worldName2WorldID.put(worldName, id);
        worldID2WorldHandle.put(id, worldHandle);

        if (activeWorld == null && worldName.equals(EngineSetting.STARTING_WORLD))
            activeWorld = worldHandle;
    }

    // Accessible \\

    public boolean hasWorld(String worldName) {
        return worldName2WorldID.containsKey(worldName);
    }

    public int getWorldIDFromWorldName(String worldName) {

        if (!worldName2WorldID.containsKey(worldName))
            request(worldName);

        if (!worldName2WorldID.containsKey(worldName))
            throwException("World not found after load: \"" + worldName + "\"");

        return worldName2WorldID.getInt(worldName);
    }

    public WorldHandle getWorldHandleFromWorldID(int worldID) {

        WorldHandle handle = worldID2WorldHandle.get(worldID);

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
        ((InternalLoader) internalLoader).request(worldName);
    }
}