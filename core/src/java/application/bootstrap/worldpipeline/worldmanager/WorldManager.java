package application.bootstrap.worldpipeline.worldmanager;

import java.io.File;

import application.bootstrap.worldpipeline.util.WorldPlacementArpgUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.world.WorldPlacementStruct;
import engine.assets.image.Pixmap;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WorldManager extends ManagerPackage {

    /*
     * Owns the world palette and drives world loading via WorldLoader.
     * Tracks the active world and exposes the standard registry API. The active
     * world defaults to the starting world on first access if not yet set.
     * Also owns every world's live hand-picked placements, seeded from its
     * companion file on first use and changed only through here: generation
     * threads read an immutable snapshot, every change bumps the placement
     * revision, and a change to a placement generation stands up bumps the
     * generation revision too, so whatever was planned from the old ones is
     * planned again while a camera moving plans nothing anew. Reloading reads
     * them back from the companion file. The editor can
     * add a virtual world, built in memory over an image it keeps painting,
     * which streams like any other but is never saved.
     */

    // Palette
    private Object2IntOpenHashMap<String> worldName2WorldID;
    private ObjectArrayList<WorldHandle> worldID2WorldHandle;

    // Active
    private WorldHandle activeWorld;

    // Placement — every world's live snapshot, guarded by worldID2Placements
    private Int2ObjectOpenHashMap<WorldPlacementStruct[]> worldID2Placements;
    private volatile int placementRevision;
    private volatile int generationRevision;

    // Internal \\

    @Override
    protected void create() {

        // Palette
        this.worldName2WorldID = RegistryUtility.createNameIndex();
        this.worldID2WorldHandle = RegistryUtility.createPalette();

        // Placement
        this.worldID2Placements = new Int2ObjectOpenHashMap<>();

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

    // Placement \\

    // Any thread — the world's current placements, never changed once handed out
    public WorldPlacementStruct[] getPlacements(WorldHandle worldHandle) {

        synchronized (worldID2Placements) {

            WorldPlacementStruct[] placements = worldID2Placements.get(worldHandle.getWorldID());

            if (placements == null) {
                placements = worldHandle.getPlacements().toArray(new WorldPlacementStruct[0]);
                worldID2Placements.put(worldHandle.getWorldID(), placements);
            }

            return placements;
        }
    }

    public void addPlacement(WorldHandle worldHandle, WorldPlacementStruct placement) {

        synchronized (worldID2Placements) {

            WorldPlacementStruct[] placements = getPlacements(worldHandle);
            WorldPlacementStruct[] grown = new WorldPlacementStruct[placements.length + 1];

            System.arraycopy(placements, 0, grown, 0, placements.length);
            grown[placements.length] = placement;

            publishPlacements(worldHandle, grown, placement.getKind().isGenerated());
        }
    }

    // One exact placement taken away — false when it no longer stands
    public boolean removePlacement(WorldHandle worldHandle, WorldPlacementStruct placement) {

        synchronized (worldID2Placements) {

            WorldPlacementStruct[] placements = getPlacements(worldHandle);

            for (int i = 0; i < placements.length; i++) {

                if (placements[i] != placement)
                    continue;

                removePlacementAt(worldHandle, placements, i);

                return true;
            }

            return false;
        }
    }

    private void removePlacementAt(WorldHandle worldHandle, WorldPlacementStruct[] placements, int index) {

        WorldPlacementStruct[] shrunk = new WorldPlacementStruct[placements.length - 1];

        System.arraycopy(placements, 0, shrunk, 0, index);
        System.arraycopy(placements, index + 1, shrunk, index, placements.length - index - 1);

        publishPlacements(worldHandle, shrunk, placements[index].getKind().isGenerated());
    }

    // One placement swapped for another in its place — false when the placement no longer stands
    public boolean replacePlacement(
            WorldHandle worldHandle,
            WorldPlacementStruct previous,
            WorldPlacementStruct replacement) {

        synchronized (worldID2Placements) {

            WorldPlacementStruct[] placements = getPlacements(worldHandle);

            for (int i = 0; i < placements.length; i++) {

                if (placements[i] != previous)
                    continue;

                WorldPlacementStruct[] replaced = placements.clone();
                replaced[i] = replacement;
                publishPlacements(
                        worldHandle,
                        replaced,
                        previous.getKind().isGenerated() || replacement.getKind().isGenerated());

                return true;
            }

            return false;
        }
    }

    // The placements read back from the companion file, dropping every unsaved change
    public boolean reloadPlacements(WorldHandle worldHandle) {

        ArpgObjectStruct worldArpg = ArpgUtility.tryLoadObject(resolveCompanionFile(worldHandle));

        if (worldArpg == null)
            return false;

        synchronized (worldID2Placements) {
            publishPlacements(worldHandle, WorldPlacementArpgUtility.parsePlacements(
                    worldArpg, worldHandle.getWorldName()).toArray(new WorldPlacementStruct[0]), true);
        }

        return true;
    }

    // The placements made current, the generation revision bumped only when generation reads what changed
    private void publishPlacements(WorldHandle worldHandle, WorldPlacementStruct[] placements, boolean generated) {

        worldID2Placements.put(worldHandle.getWorldID(), placements);
        placementRevision++;

        if (generated)
            generationRevision++;
    }

    private File resolveCompanionFile(WorldHandle worldHandle) {
        return ArpgUtility.resolveCompanionFile(worldHandle.getWorldFile());
    }

    public int getPlacementRevision() {
        return placementRevision;
    }

    public int getGenerationRevision() {
        return generationRevision;
    }

    // On-Demand \\

    public void request(String worldName) {
        ((WorldLoader) internalLoader).request(worldName);
    }

    // A world held only in memory over an image its owner keeps, registered by name like any other
    public WorldHandle createVirtualWorld(String worldName, Pixmap world) {

        if (hasWorld(worldName))
            throwException("Cannot create virtual world \"" + worldName + "\": a world by that name exists.");

        return ((WorldLoader) internalLoader).buildVirtual(worldName, world);
    }
}