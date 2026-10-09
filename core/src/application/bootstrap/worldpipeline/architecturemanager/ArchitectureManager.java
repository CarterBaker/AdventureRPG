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
     * Architecture IDs are assigned in registration order.
     */

    // Palette
    private Object2IntOpenHashMap<String> architectureName2ArchitectureID;
    private ObjectArrayList<ArchitectureHandle> architectureID2ArchitectureHandle;
    private ObjectArrayList<ArchitectureHandle> architectureHandles;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.architectureName2ArchitectureID = RegistryUtility.createNameIndex();
        this.architectureID2ArchitectureHandle = RegistryUtility.createPalette();
        this.architectureHandles = new ObjectArrayList<>();

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
}
