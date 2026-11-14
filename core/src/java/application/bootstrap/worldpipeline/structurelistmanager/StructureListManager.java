package application.bootstrap.worldpipeline.structurelistmanager;

import application.bootstrap.worldpipeline.structure.StructureHandle;
import application.bootstrap.worldpipeline.structurelist.StructureListHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.extras.WeightedTableUtility;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class StructureListManager extends ManagerPackage {

    /*
     * Owns the structure list palette: weighted lists of structures an
     * architecture fills each role of a layout from, so a house lot draws
     * one of many houses and a keep one of a few keeps, and is the one way a
     * structure is drawn from one. Every list is resolved in awake(), before
     * any chunk generates, so worker threads only ever read the palette.
     * Structure list IDs are assigned in registration order.
     */

    // Palette
    private Object2IntOpenHashMap<String> structureListName2StructureListID;
    private ObjectArrayList<StructureListHandle> structureListID2StructureListHandle;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.structureListName2StructureListID = RegistryUtility.createNameIndex();
        this.structureListID2StructureListHandle = RegistryUtility.createPalette();

        create(StructureListLoader.class);
    }

    @Override
    protected void awake() {
        internalLoader.requestAll();
    }

    // Management \\

    short registerStructureListName(String structureListName) {
        return (short) RegistryUtility.registerID(
                structureListName2StructureListID, structureListID2StructureListHandle, structureListName,
                EngineSetting.REGISTRY_SHORT_ID_COUNT);
    }

    void addStructureListHandle(StructureListHandle structureListHandle) {

        short structureListID = structureListHandle.getStructureListID();

        if (structureListID2StructureListHandle.get(structureListID) != null)
            throwException("Duplicate structure list name: '" + structureListHandle.getStructureListName()
                    + "' was registered more than once");

        structureListID2StructureListHandle.set(structureListID, structureListHandle);
    }

    // On-Demand \\

    public void request(String structureListName) {
        ((StructureListLoader) internalLoader).request(structureListName);
    }

    // Draw \\

    // The structure a roll from 0 up to 1 draws from a list, each as likely as its weight
    public StructureHandle drawStructure(StructureListHandle structureListHandle, float roll) {
        return structureListHandle.getStructures()[WeightedTableUtility.pick(
                structureListHandle.getCumulativeWeights(), roll)];
    }

    // Accessible \\

    public boolean hasStructureList(String structureListName) {
        return RegistryUtility.getHandle(
                structureListName2StructureListID, structureListID2StructureListHandle, structureListName) != null;
    }

    public StructureListHandle getStructureListHandleFromStructureListName(String structureListName) {

        StructureListHandle handle = RegistryUtility.getHandle(
                structureListName2StructureListID, structureListID2StructureListHandle, structureListName);

        if (handle == null) {
            request(structureListName);
            handle = RegistryUtility.getHandle(
                    structureListName2StructureListID, structureListID2StructureListHandle, structureListName);
        }

        if (handle == null)
            throwException("Structure list \"" + structureListName + "\" was not registered after its on-demand "
                    + "load completed — check for a resource-name/path mismatch in the structure list directory.");

        return handle;
    }
}
