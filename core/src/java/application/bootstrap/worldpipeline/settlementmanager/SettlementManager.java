package application.bootstrap.worldpipeline.settlementmanager;

import java.util.Comparator;

import application.bootstrap.worldpipeline.architecture.ArchitectureHandle;
import application.bootstrap.worldpipeline.architecturemanager.ArchitectureManager;
import application.bootstrap.worldpipeline.layout.LayoutPlanStruct;
import application.bootstrap.worldpipeline.layoutmanager.LayoutManager;
import application.bootstrap.worldpipeline.settlement.SettlementHandle;
import application.bootstrap.worldpipeline.settlement.SettlementPlanStruct;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.world.WorldPlacementStruct;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.root.UtilityPackage.InternalException;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.extras.WeightedTableUtility;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;

public class SettlementManager extends ManagerPackage {

    /*
     * Owns the settlement palette, every type of settlement the world can
     * grow, and is world generation's single entry point for settlements and
     * the roads between them. The world is divided into settlement cells;
     * each holds at most one settlement, placed by hand or grown where its
     * biome allows an architecture, and neighbouring settlements are joined
     * by roads east and south. Sites, plans and roads are settled lazily and
     * cached, so a chunk lays, distant terrain draws, and trees and
     * self-placing structures keep off exactly the same layouts. Every
     * settlement type is resolved in awake(), and each architecture's
     * weighted table of the types it builds is published then, before any
     * chunk generates. A settlement type edited live keeps its handle and ID
     * and takes the new data, the tables are rebuilt, and the revision rises
     * so every cell is settled again. The editor may plan any hand placement
     * afresh, uncached, to preview it before or after it stands. Settlement
     * IDs are assigned in registration order.
     */

    // Internal
    private ArchitectureManager architectureManager;
    private LayoutManager layoutManager;
    private SettlementSiteBranch settlementSiteBranch;
    private SettlementPlanBranch settlementPlanBranch;
    private SettlementCollectBranch settlementCollectBranch;

    // Palette
    private Object2IntOpenHashMap<String> settlementName2SettlementID;
    private ObjectArrayList<SettlementHandle> settlementID2SettlementHandle;
    private ObjectArrayList<SettlementHandle> settlementHandles;

    // Draw — per architecture, the types it builds and their running weight totals, replaced whole on a rebuild
    private volatile Short2ObjectOpenHashMap<SettlementDrawStruct> architectureID2Draw;

    // Live Edit
    private volatile int revision;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.settlementName2SettlementID = RegistryUtility.createNameIndex();
        this.settlementID2SettlementHandle = RegistryUtility.createPalette();
        this.settlementHandles = new ObjectArrayList<>();

        // Draw
        this.architectureID2Draw = new Short2ObjectOpenHashMap<>();

        create(SettlementLoader.class);
        this.settlementSiteBranch = create(SettlementSiteBranch.class);
        create(SettlementTrailBranch.class);
        this.settlementPlanBranch = create(SettlementPlanBranch.class);
        create(SettlementLinkBranch.class);
        create(SettlementCacheBranch.class);
        this.settlementCollectBranch = create(SettlementCollectBranch.class);
    }

    @Override
    protected void get() {
        this.architectureManager = get(ArchitectureManager.class);
        this.layoutManager = get(LayoutManager.class);
    }

    @Override
    protected void awake() {

        internalLoader.requestAll();
        settlementHandles.sort(Comparator.comparing(SettlementHandle::getSettlementName));
        buildDrawTables();
    }

    // Management \\

    short registerSettlementName(String settlementName) {
        return (short) RegistryUtility.registerID(
                settlementName2SettlementID, settlementID2SettlementHandle, settlementName,
                EngineSetting.REGISTRY_SHORT_ID_COUNT);
    }

    void addSettlementHandle(SettlementHandle settlementHandle) {

        short settlementID = settlementHandle.getSettlementID();

        if (settlementID2SettlementHandle.get(settlementID) != null)
            throwException("Duplicate settlement name: '" + settlementHandle.getSettlementName()
                    + "' was registered more than once");

        settlementID2SettlementHandle.set(settlementID, settlementHandle);
        settlementHandles.add(settlementHandle);
    }

    // Each architecture's types, in name order, with weights above zero, ready to draw from, published at once
    private void buildDrawTables() {

        ObjectArrayList<ArchitectureHandle> architectures = architectureManager.getArchitectureHandles();
        Short2ObjectOpenHashMap<SettlementDrawStruct> draws = new Short2ObjectOpenHashMap<>();

        for (int i = 0; i < architectures.size(); i++) {

            ArchitectureHandle architectureHandle = architectures.get(i);
            ObjectArrayList<SettlementHandle> built = new ObjectArrayList<>();
            FloatArrayList weights = new FloatArrayList();

            for (int j = 0; j < settlementHandles.size(); j++) {

                SettlementHandle settlementHandle = settlementHandles.get(j);

                if (settlementHandle.getWeight() <= 0f
                        || !architectureHandle.buildsSettlement(settlementHandle.getSettlementID()))
                    continue;

                built.add(settlementHandle);
                weights.add(settlementHandle.getWeight());
            }

            if (built.isEmpty())
                continue;

            draws.put(architectureHandle.getArchitectureID(), new SettlementDrawStruct(
                    built.toArray(new SettlementHandle[0]),
                    WeightedTableUtility.buildCumulative(weights, architectureHandle.getArchitectureName())));
        }

        this.architectureID2Draw = draws;
    }

    // Draw \

    // The type an architecture grows for a roll from 0 up to 1, null when it grows none on its own
    SettlementHandle drawSettlement(ArchitectureHandle architectureHandle, float roll) {

        SettlementDrawStruct draw = architectureID2Draw.get(architectureHandle.getArchitectureID());

        if (draw == null)
            return null;

        return draw.getSettlements()[WeightedTableUtility.pick(draw.getCumulativeWeights(), roll)];
    }

    // Live Edit \

    // Throws a catchable InternalException, leaving the settlement as it was, when the edit cannot go live
    public synchronized void rebuildSettlement(String settlementName, ArpgObjectStruct settlementArpg) {

        SettlementHandle settlementHandle = RegistryUtility.getHandle(
                settlementName2SettlementID, settlementID2SettlementHandle, settlementName);

        if (settlementHandle == null)
            throw new InternalException("Settlement \"" + settlementName
                    + "\" is not a loaded settlement — only a settlement the game started with can be edited live.");

        settlementHandle.constructor(SettlementArpgUtility.parse(
                settlementName, settlementHandle.getSettlementID(), settlementArpg));
        buildDrawTables();
        revision++;
    }

    // Raised by every live edit, so whatever was settled from the old types is settled again
    public int getRevision() {
        return revision;
    }

    // On-Demand \\

    public void request(String settlementName) {
        ((SettlementLoader) internalLoader).request(settlementName);
    }

    // Generation \\

    // Worker — every settlement and road between settlements reaching a freshly generated chunk, laid into it
    public void generateSettlements(WorldHandle worldHandle, long chunkCoordinate, SubChunkInstance[] subChunks) {

        double half = EngineSetting.CHUNK_SIZE * 0.5;
        ObjectArrayList<LayoutPlanStruct> layouts = settlementCollectBranch.collectAround(
                worldHandle,
                (double) Coordinate2Long.unpackX(chunkCoordinate) * EngineSetting.CHUNK_SIZE + half,
                (double) Coordinate2Long.unpackY(chunkCoordinate) * EngineSetting.CHUNK_SIZE + half,
                half);

        for (int i = 0; i < layouts.size(); i++)
            layoutManager.stampLayout(worldHandle, chunkCoordinate, subChunks, layouts.get(i));
    }

    // Any thread — true when a settlement or road claims ground within a margin of a world column
    public boolean isClaimed(WorldHandle worldHandle, double worldX, double worldZ, double marginBlocks) {
        return layoutManager.isClaimed(
                worldHandle,
                settlementCollectBranch.collectAround(worldHandle, worldX, worldZ, marginBlocks),
                worldX, worldZ, marginBlocks);
    }

    // Any thread — every settlement and road reaching a rectangle of world blocks
    public void collectLayouts(
            WorldHandle worldHandle,
            double minX,
            double minZ,
            double maxX,
            double maxZ,
            ObjectArrayList<LayoutPlanStruct> out) {
        settlementCollectBranch.collect(worldHandle, minX, minZ, maxX, maxZ, out);
    }

    // Editor \\

    // Any thread — the settlement a hand placement stands up, planned afresh wherever it is and never cached
    public SettlementPlanStruct planPlacement(WorldHandle worldHandle, WorldPlacementStruct placement) {
        return settlementPlanBranch.plan(
                worldHandle, settlementSiteBranch.resolvePlacementSite(worldHandle, placement));
    }

    // The settlement placed by hand in the settlement cell a world column falls in, null when none is
    public WorldPlacementStruct findPlacedSettlement(WorldHandle worldHandle, long worldX, long worldZ) {
        return settlementSiteBranch.findPlacedSettlement(
                worldHandle,
                settlementSiteBranch.resolveCell(WorldWrapUtility.wrapBlockX(worldHandle, worldX)),
                settlementSiteBranch.resolveCell(WorldWrapUtility.wrapBlockZ(worldHandle, worldZ)));
    }

    // Accessible \\

    public boolean hasSettlement(String settlementName) {
        return RegistryUtility.getHandle(
                settlementName2SettlementID, settlementID2SettlementHandle, settlementName) != null;
    }

    public SettlementHandle getSettlementHandleFromSettlementName(String settlementName) {

        SettlementHandle handle = RegistryUtility.getHandle(
                settlementName2SettlementID, settlementID2SettlementHandle, settlementName);

        if (handle == null) {
            request(settlementName);
            handle = RegistryUtility.getHandle(
                    settlementName2SettlementID, settlementID2SettlementHandle, settlementName);
        }

        if (handle == null)
            throwException("Settlement \"" + settlementName + "\" was not registered after its on-demand load "
                    + "completed — check for a resource-name/path mismatch in the settlement directory.");

        return handle;
    }

    public short getSettlementIDFromSettlementName(String settlementName) {
        return getSettlementHandleFromSettlementName(settlementName).getSettlementID();
    }

    public ObjectArrayList<SettlementHandle> getSettlementHandles() {
        return settlementHandles;
    }
}
