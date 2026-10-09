package application.bootstrap.worldpipeline.structuremanager;

import java.util.Arrays;
import java.util.Comparator;

import application.bootstrap.worldpipeline.structure.StructureHandle;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class StructureManager extends ManagerPackage {

    /*
     * Owns the structure palette and is world generation's single entry point
     * for stamping structures into a chunk. Every structure is resolved on
     * demand in awake(), before any chunk generates, and published to worker
     * threads as an immutable name-sorted snapshot so every chunk walks
     * structures in the same order without locking. Structure IDs are
     * assigned in registration order.
     */

    // Internal
    private StructurePlacementBranch structurePlacementBranch;

    // Palette
    private Object2IntOpenHashMap<String> structureName2StructureID;
    private ObjectArrayList<StructureHandle> structureID2StructureHandle;
    private ObjectArrayList<StructureHandle> structureHandleList;

    // Generation Snapshot
    private volatile StructureHandle[] structureHandles;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.structureName2StructureID = RegistryUtility.createNameIndex();
        this.structureID2StructureHandle = RegistryUtility.createPalette();
        this.structureHandleList = new ObjectArrayList<>();

        // Generation Snapshot
        this.structureHandles = new StructureHandle[0];

        create(StructureLoader.class);
        this.structurePlacementBranch = create(StructurePlacementBranch.class);
    }

    @Override
    protected void awake() {
        internalLoader.requestAll();
    }

    // Management \\

    short registerStructureName(String structureName) {
        return (short) RegistryUtility.registerID(
                structureName2StructureID, structureID2StructureHandle, structureName,
                EngineSetting.REGISTRY_SHORT_ID_COUNT);
    }

    void addStructureHandle(StructureHandle structureHandle) {

        short structureID = structureHandle.getStructureID();

        if (structureID2StructureHandle.get(structureID) != null)
            throwException("Duplicate structure name: '" + structureHandle.getStructureName()
                    + "' was registered more than once");

        structureID2StructureHandle.set(structureID, structureHandle);
        structureHandleList.add(structureHandle);

        publishSnapshot();
    }

    private void publishSnapshot() {

        StructureHandle[] snapshot = structureHandleList.toArray(new StructureHandle[0]);
        Arrays.sort(snapshot, Comparator.comparing(StructureHandle::getStructureName));

        structureHandles = snapshot;
    }

    // On-Demand \\

    public void request(String structureName) {
        ((StructureLoader) internalLoader).request(structureName);
    }

    // Generation \\

    public void generateStructures(WorldHandle worldHandle, long chunkCoordinate, SubChunkInstance[] subChunks) {
        structurePlacementBranch.generateStructures(worldHandle, chunkCoordinate, subChunks, structureHandles);
    }

    // Accessible \\

    public boolean hasStructure(String structureName) {
        return RegistryUtility.getHandle(
                structureName2StructureID, structureID2StructureHandle, structureName) != null;
    }

    public StructureHandle getStructureHandleFromStructureName(String structureName) {

        StructureHandle handle = RegistryUtility.getHandle(
                structureName2StructureID, structureID2StructureHandle, structureName);

        if (handle == null) {
            request(structureName);
            handle = RegistryUtility.getHandle(
                    structureName2StructureID, structureID2StructureHandle, structureName);
        }

        if (handle == null)
            throwException("Structure \"" + structureName + "\" was not registered after its on-demand load "
                    + "completed — check for a resource-name/path mismatch in the structure directory.");

        return handle;
    }

    public short getStructureIDFromStructureName(String structureName) {
        return getStructureHandleFromStructureName(structureName).getStructureID();
    }

    public StructureHandle getStructureHandleFromStructureID(short structureID) {

        StructureHandle handle = RegistryUtility.getHandle(structureID2StructureHandle, structureID);

        if (handle == null)
            throwException("No handle registered for structure ID: " + structureID);

        return handle;
    }
}
