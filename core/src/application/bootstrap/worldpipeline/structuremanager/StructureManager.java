package application.bootstrap.worldpipeline.structuremanager;

import java.util.concurrent.ConcurrentHashMap;

import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.structure.StructureHandle;
import application.bootstrap.worldpipeline.structure.StructureType;
import application.bootstrap.worldpipeline.structure.StructureWriteAsyncContainer;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class StructureManager extends ManagerPackage {

    /*
     * Owns the structure catalogue and is the one entry point world
     * generation talks to. Definitions load through the usual
     * loader/builder pair — batched in the background from boot, and
     * on demand the instant anything asks for one by name — and live in
     * lock-free registries because generation workers read them
     * concurrently. The self-placing subset of the catalogue (anything with
     * a spawn rule or hand-authored locations) is known from the loader's
     * scan and resolved as a whole the first time placement needs it.
     *
     * The work itself is split across branches: StructurePlacementBranch
     * decides where structures are and which of them the road network
     * joins, StructureLayoutBranch turns a placement into the streets and
     * templates it writes, RoadPlanBranch finds and shapes every road and
     * street path, and StructureStampBranch rasterizes all of it into the
     * block overrides for one chunk column. resolveChunk() is called by
     * WorldGenerationManager once per chunk, after the column's terrain is
     * known and before its subchunks generate.
     */

    // Registry
    private final ConcurrentHashMap<String, StructureHandle> structureName2StructureHandle = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Short, StructureHandle> structureID2StructureHandle = new ConcurrentHashMap<>();

    // Self-placing catalogue — names from the loader's scan, handles resolved lazily
    private ObjectArrayList<String> placeableNames = new ObjectArrayList<>();
    private volatile StructureHandle[] placeableHandles;

    // Internal
    private BlockManager blockManager;
    private StructureStampBranch stampBranch;
    private StructureWriteAsyncContainer writeContainer;

    private short airBlockID;

    // Base \\

    @Override
    protected void create() {
        create(StructureLoader.class);
        create(StructurePlacementBranch.class);
        create(StructureLayoutBranch.class);
        create(RoadPlanBranch.class);
        this.stampBranch = create(StructureStampBranch.class);
        this.writeContainer = create(StructureWriteAsyncContainer.class);
    }

    @Override
    protected void get() {
        this.blockManager = get(BlockManager.class);
    }

    @Override
    protected void awake() {
        this.airBlockID = (short) blockManager.getBlockIDFromBlockName(EngineSetting.AIR_BLOCK_NAME);
    }

    // Management \\

    synchronized void addStructure(StructureHandle structureHandle) {

        StructureHandle existing = structureID2StructureHandle.get(structureHandle.getStructureID());

        if (existing != null && RegistryUtility.isCollision(
                structureHandle.getStructureName(), existing.getStructureName(), structureHandle.getStructureID()))
            throwException("Structure ID collision: '"
                    + structureHandle.getStructureName() + "' collides with '"
                    + existing.getStructureName() + "' (ID " + structureHandle.getStructureID()
                    + ") — rename one structure to resolve");

        structureName2StructureHandle.put(structureHandle.getStructureName(), structureHandle);
        structureID2StructureHandle.put(structureHandle.getStructureID(), structureHandle);
    }

    void registerPlaceableNames(ObjectArrayList<String> names) {
        this.placeableNames = names;
    }

    // On-Demand \\

    public void request(String structureName) {
        ((StructureLoader) internalLoader).request(structureName);
    }

    // Chunk Resolution \\

    /*
     * Fills the calling worker's write container with every block the
     * structures and roads reaching this chunk column want written.
     * groundHeightBlocks is the column's own per-block-column ground height
     * from WorldGenerationManager, so foundations and pillars stop exactly
     * on the terrain generation is about to produce.
     */
    public StructureWriteAsyncContainer resolveChunk(
            WorldHandle worldHandle,
            long chunkCoordinate,
            int[] groundHeightBlocks) {

        StructureWriteAsyncContainer writes = writeContainer.getInstance();

        writes.begin(chunkCoordinate);
        stampBranch.stampChunk(worldHandle, chunkCoordinate, groundHeightBlocks, writes);

        return writes;
    }

    // Accessible \\

    /*
     * Every structure that can place itself, resolved on first use and in
     * the loader's sorted scan order, so every worker enumerates candidates
     * in the same order and settles ties identically.
     */
    public StructureHandle[] getPlaceableHandles() {

        StructureHandle[] handles = placeableHandles;

        if (handles != null)
            return handles;

        synchronized (this) {

            if (placeableHandles != null)
                return placeableHandles;

            ObjectArrayList<StructureHandle> resolved = new ObjectArrayList<>();

            for (int i = 0; i < placeableNames.size(); i++) {

                StructureHandle handle = getStructureHandleFromStructureName(placeableNames.get(i));

                if (!handle.getStructureType().isPlaceable())
                    throwException("Structure \"" + handle.getStructureName() + "\" is a "
                            + handle.getStructureType() + " but declares \"spawn\" or \"locations\" — "
                            + "only STRUCTURE, DUNGEON and SETTLEMENT definitions can place themselves.");

                resolved.add(handle);
            }

            placeableHandles = resolved.toArray(new StructureHandle[0]);

            return placeableHandles;
        }
    }

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

    // Resolves a reference that must be of one specific type, failing loudly when it isn't.
    public StructureHandle getStructureHandleOfType(String structureName, StructureType type, String referencedBy) {

        StructureHandle handle = getStructureHandleFromStructureName(structureName);

        if (handle.getStructureType() != type)
            throwException("\"" + referencedBy + "\" references \"" + structureName + "\" as a " + type
                    + ", but it is a " + handle.getStructureType() + ".");

        return handle;
    }

    public StructureHandle getStructureHandleFromStructureID(short structureID) {

        StructureHandle handle = structureID2StructureHandle.get(structureID);

        if (handle == null)
            throwException("No handle registered for structure ID: " + structureID);

        return handle;
    }

    short getAirBlockID() {
        return airBlockID;
    }
}
