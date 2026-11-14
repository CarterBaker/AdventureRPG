package application.bootstrap.worldpipeline.cavebiomemanager;

import application.bootstrap.worldpipeline.cavebiome.CaveBiomeHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class CaveBiomeManager extends ManagerPackage {

    /*
     * Owns the cave biome palette by name and ID. A cave biome dresses the
     * regions of the underground a surface biome lists it for: their rock,
     * floors, ceilings, coverings, stalactites and stalagmites and veins.
     * Every cave biome is loaded by awake(), before any chunk generates, so
     * the streaming threads only ever read the palette; a biome that names
     * one while it builds loads it on demand. IDs are assigned in
     * registration order.
     */

    // Palette
    private Object2IntOpenHashMap<String> caveBiomeName2CaveBiomeID;
    private ObjectArrayList<CaveBiomeHandle> caveBiomeID2CaveBiomeHandle;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.caveBiomeName2CaveBiomeID = RegistryUtility.createNameIndex();
        this.caveBiomeID2CaveBiomeHandle = RegistryUtility.createPalette();

        create(CaveBiomeLoader.class);
    }

    @Override
    protected void awake() {
        internalLoader.requestAll();
    }

    // Management \\

    synchronized short registerCaveBiomeName(String caveBiomeName) {
        return (short) RegistryUtility.registerID(
                caveBiomeName2CaveBiomeID, caveBiomeID2CaveBiomeHandle, caveBiomeName,
                EngineSetting.REGISTRY_SHORT_ID_COUNT);
    }

    synchronized void addCaveBiomeHandle(CaveBiomeHandle caveBiomeHandle) {

        short caveBiomeID = caveBiomeHandle.getCaveBiomeID();

        if (caveBiomeID2CaveBiomeHandle.get(caveBiomeID) != null)
            throwException("Duplicate cave biome name: '" + caveBiomeHandle.getCaveBiomeName()
                    + "' was registered more than once");

        caveBiomeID2CaveBiomeHandle.set(caveBiomeID, caveBiomeHandle);
    }

    // On-Demand \\

    public void request(String caveBiomeName) {
        ((CaveBiomeLoader) internalLoader).request(caveBiomeName);
    }

    // Accessible \\

    public boolean hasCaveBiome(String caveBiomeName) {
        return findCaveBiomeHandle(caveBiomeName) != null;
    }

    // A loaded cave biome by name, null when none carries it — never loads, so any thread may ask
    public CaveBiomeHandle findCaveBiomeHandle(String caveBiomeName) {
        return RegistryUtility.getHandle(caveBiomeName2CaveBiomeID, caveBiomeID2CaveBiomeHandle, caveBiomeName);
    }

    public CaveBiomeHandle getCaveBiomeHandleFromCaveBiomeName(String caveBiomeName) {

        CaveBiomeHandle handle = findCaveBiomeHandle(caveBiomeName);

        if (handle == null) {
            request(caveBiomeName);
            handle = findCaveBiomeHandle(caveBiomeName);
        }

        if (handle == null)
            throwException("Cave biome \"" + caveBiomeName
                    + "\" was not registered after its on-demand load completed — "
                    + "check for a resource-name/path mismatch in the cave biome directory.");

        return handle;
    }

    public CaveBiomeHandle getCaveBiomeHandleFromCaveBiomeID(short caveBiomeID) {

        CaveBiomeHandle handle = RegistryUtility.getHandle(caveBiomeID2CaveBiomeHandle, caveBiomeID);

        if (handle == null)
            throwException("No handle registered for cave biome ID: " + caveBiomeID);

        return handle;
    }
}
