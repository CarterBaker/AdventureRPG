package application.bootstrap.worldpipeline.architecturemanager;

import application.bootstrap.worldpipeline.architecture.ArchitectureHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ArchitectureManager extends ManagerPackage {

    /*
     * Owns the architecture palette, the building styles biomes name to say
     * what may grow on them and that every settlement and the roads between
     * them are built in. Every architecture is resolved in awake(), before
     * any chunk generates, so worker threads only ever read the palette.
     * Architecture IDs are assigned in registration order. It also owns the
     * block slots, the named places a structure lays a building material at
     * ("siding/wall") that an architecture fills with a block of its own,
     * registered by whichever structure or architecture names one first.
     */

    // Palette
    private Object2IntOpenHashMap<String> architectureName2ArchitectureID;
    private ObjectArrayList<ArchitectureHandle> architectureID2ArchitectureHandle;
    private ObjectArrayList<ArchitectureHandle> architectureHandles;

    // Block Slots
    private Object2IntOpenHashMap<String> blockSlotName2BlockSlotID;
    private ObjectArrayList<String> blockSlotID2BlockSlotName;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.architectureName2ArchitectureID = RegistryUtility.createNameIndex();
        this.architectureID2ArchitectureHandle = RegistryUtility.createPalette();
        this.architectureHandles = new ObjectArrayList<>();

        // Block Slots
        this.blockSlotName2BlockSlotID = RegistryUtility.createNameIndex();
        this.blockSlotID2BlockSlotName = RegistryUtility.createPalette();

        create(ArchitectureLoader.class);
    }

    @Override
    protected void awake() {
        internalLoader.requestAll();
    }

    // Management \\

    short registerArchitectureName(String architectureName) {
        return (short) RegistryUtility.registerID(
                architectureName2ArchitectureID, architectureID2ArchitectureHandle, architectureName,
                EngineSetting.REGISTRY_SHORT_ID_COUNT);
    }

    void addArchitectureHandle(ArchitectureHandle architectureHandle) {

        short architectureID = architectureHandle.getArchitectureID();

        if (architectureID2ArchitectureHandle.get(architectureID) != null)
            throwException("Duplicate architecture name: '" + architectureHandle.getArchitectureName()
                    + "' was registered more than once");

        architectureID2ArchitectureHandle.set(architectureID, architectureHandle);
        architectureHandles.add(architectureHandle);
    }

    // A block slot's ID, registered on first use; a slot is named as its group and its place, "group/place"
    public short registerBlockSlotName(String blockSlotName, String ownerName) {

        int separator = blockSlotName.indexOf(EngineSetting.ARCHITECTURE_SLOT_SEPARATOR);

        if (separator <= 0 || separator == blockSlotName.length() - 1
                || blockSlotName.indexOf(EngineSetting.ARCHITECTURE_SLOT_SEPARATOR, separator + 1) >= 0)
            throwException("\"" + ownerName + "\" names block slot \"" + blockSlotName
                    + "\" — a block slot is named as its group and its place, \"group/place\".");

        return (short) RegistryUtility.registerHandle(
                blockSlotName2BlockSlotID, blockSlotID2BlockSlotName, blockSlotName, blockSlotName,
                EngineSetting.REGISTRY_SHORT_ID_COUNT);
    }

    // On-Demand \\

    public void request(String architectureName) {
        ((ArchitectureLoader) internalLoader).request(architectureName);
    }

    // Accessible \\

    public boolean hasArchitecture(String architectureName) {
        return RegistryUtility.getHandle(
                architectureName2ArchitectureID, architectureID2ArchitectureHandle, architectureName) != null;
    }

    public ArchitectureHandle getArchitectureHandleFromArchitectureName(String architectureName) {

        ArchitectureHandle handle = RegistryUtility.getHandle(
                architectureName2ArchitectureID, architectureID2ArchitectureHandle, architectureName);

        if (handle == null) {
            request(architectureName);
            handle = RegistryUtility.getHandle(
                    architectureName2ArchitectureID, architectureID2ArchitectureHandle, architectureName);
        }

        if (handle == null)
            throwException("Architecture \"" + architectureName + "\" was not registered after its on-demand "
                    + "load completed — check for a resource-name/path mismatch in the architecture directory.");

        return handle;
    }

    public short getArchitectureIDFromArchitectureName(String architectureName) {
        return getArchitectureHandleFromArchitectureName(architectureName).getArchitectureID();
    }

    public ArchitectureHandle getArchitectureHandleFromArchitectureID(short architectureID) {

        ArchitectureHandle handle = RegistryUtility.getHandle(architectureID2ArchitectureHandle, architectureID);

        if (handle == null)
            throwException("No handle registered for architecture ID: " + architectureID);

        return handle;
    }

    public ObjectArrayList<ArchitectureHandle> getArchitectureHandles() {
        return architectureHandles;
    }

    // Every block slot any structure or architecture names by ID, the reserved slot 0 null
    public ObjectArrayList<String> getBlockSlotNames() {
        return blockSlotID2BlockSlotName;
    }
}
