package application.bootstrap.worldpipeline.structuremanager;

import java.util.Arrays;
import java.util.Comparator;

import application.bootstrap.worldpipeline.structure.StructureHandle;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;

public class StructureManager extends ManagerPackage {

    /*
     * Owns the structure palette and is world generation's single entry point
     * for stamping structures into a chunk. Every structure is resolved on
     * demand in awake(), before any chunk generates, and published to worker
     * threads as an immutable name-sorted snapshot so every chunk walks
     * structures in the same order without locking.
     */

    // Internal
    private StructurePlacementBranch structurePlacementBranch;

    // Palette
    private Object2ObjectOpenHashMap<String, StructureHandle> structureName2StructureHandle;
    private Short2ObjectOpenHashMap<StructureHandle> structureID2StructureHandle;

    // Generation Snapshot
    private volatile StructureHandle[] structureHandles;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.structureName2StructureHandle = new Object2ObjectOpenHashMap<>();
        this.structureID2StructureHandle = new Short2ObjectOpenHashMap<>();

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

    void addStructureHandle(StructureHandle structureHandle) {

        StructureHandle existing = structureID2StructureHandle.get(structureHandle.getStructureID());

        if (existing != null && RegistryUtility.isCollision(structureHandle.getStructureName(),
                existing.getStructureName(), structureHandle.getStructureID()))
            throwException("Structure ID collision: '"
                    + structureHandle.getStructureName() + "' collides with '"
                    + existing.getStructureName() + "' (ID " + structureHandle.getStructureID()
                    + ") — rename one structure to resolve");

        structureName2StructureHandle.put(structureHandle.getStructureName(), structureHandle);
        structureID2StructureHandle.put(structureHandle.getStructureID(), structureHandle);

        publishSnapshot();
    }

    private void publishSnapshot() {

        StructureHandle[] snapshot = structureName2StructureHandle.values().toArray(new StructureHandle[0]);
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
        return structureName2StructureHandle.containsKey(structureName);
    }

    public StructureHandle getStructureHandleFromStructureName(String structureName) {

        StructureHandle handle = structureName2StructureHandle.get(structureName);

        if (handle == null) {
            request(structureName);
            handle = structureName2StructureHandle.get(structureName);
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

        StructureHandle handle = structureID2StructureHandle.get(structureID);

        if (handle == null)
            throwException("No handle registered for structure ID: " + structureID);

        return handle;
    }
}
