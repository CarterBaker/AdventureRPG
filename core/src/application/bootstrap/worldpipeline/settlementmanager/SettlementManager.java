package application.bootstrap.worldpipeline.settlementmanager;

import java.util.Comparator;

import application.bootstrap.worldpipeline.architecture.ArchitectureHandle;
import application.bootstrap.worldpipeline.architecturemanager.ArchitectureManager;
import application.bootstrap.worldpipeline.layout.LayoutPlanStruct;
import application.bootstrap.worldpipeline.layoutmanager.LayoutManager;
import application.bootstrap.worldpipeline.settlement.SettlementHandle;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
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
     * chunk generates. Settlement IDs are assigned in registration order.
     */

    // Internal
    private ArchitectureManager architectureManager;
    private LayoutManager layoutManager;
    private SettlementCollectBranch settlementCollectBranch;

    // Palette
    private Object2IntOpenHashMap<String> settlementName2SettlementID;
    private ObjectArrayList<SettlementHandle> settlementID2SettlementHandle;
    private ObjectArrayList<SettlementHandle> settlementHandles;

    // Draw — per architecture, the types it builds and their running weight totals
    private Short2ObjectOpenHashMap<SettlementHandle[]> architectureID2Settlements;
    private Short2ObjectOpenHashMap<float[]> architectureID2Weights;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.settlementName2SettlementID = RegistryUtility.createNameIndex();
        this.settlementID2SettlementHandle = RegistryUtility.createPalette();
        this.settlementHandles = new ObjectArrayList<>();

        // Draw
        this.architectureID2Settlements = new Short2ObjectOpenHashMap<>();
        this.architectureID2Weights = new Short2ObjectOpenHashMap<>();

        create(SettlementLoader.class);
        create(SettlementSiteBranch.class);
        create(SettlementTrailBranch.class);
        create(SettlementPlanBranch.class);
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

    // Each architecture's types, in name order, with weights above zero, ready to draw from
    private void buildDrawTables() {

        ObjectArrayList<ArchitectureHandle> architectures = architectureManager.getArchitectureHandles();

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

            architectureID2Settlements.put(
                    architectureHandle.getArchitectureID(), built.toArray(new SettlementHandle[0]));
            architectureID2Weights.put(
                    architectureHandle.getArchitectureID(),
                    WeightedTableUtility.buildCumulative(weights, architectureHandle.getArchitectureName()));
        }
    }

    // Draw \\

    // The type an architecture grows for a roll from 0 up to 1, null when it grows none on its own
    SettlementHandle drawSettlement(ArchitectureHandle architectureHandle, float roll) {

        SettlementHandle[] settlements = architectureID2Settlements.get(architectureHandle.getArchitectureID());

        if (settlements == null)
            return null;

        return settlements[WeightedTableUtility.pick(
                architectureID2Weights.get(architectureHandle.getArchitectureID()), roll)];
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
