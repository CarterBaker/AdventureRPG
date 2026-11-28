package application.bootstrap.oceanpipeline.exposuremanager;

import java.util.Arrays;

import application.bootstrap.oceanpipeline.exposure.ExposureInstance;
import application.bootstrap.oceanpipeline.util.OceanWaveUtility;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.world.GlobeFrameStruct;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

public class ExposureManager extends ManagerPackage {

    /*
     * Owns how exposed every body of water is, the measure that keeps a pond
     * glassy while the open sea builds whitecaps. The world is cut into
     * fixed cells, each probed once for its share of open water and cached,
     * nearest the grid first under a per-frame budget. A cell's exposure is
     * the open-water share across the cells around it, so a small pond, a
     * sheltered bay and the open ocean each read as the size of water they
     * are, shaped by OceanWaveUtility.resolveExposure() and eased into every
     * grid's ExposureInstance. Runs in LATE_UPDATE, once the grid's reference
     * chunk is final, since every window is placed relative to it. On a torus
     * a cell is the world's own, wrapped; on a globe it is the face cell
     * under the window cell's centre, read through the grid's chart, so the
     * cache is shared by every grid however its chart lies.
     */

    // Internal
    private WorldStreamManager worldStreamManager;
    private ExposureProbeBranch exposureProbeBranch;

    // Palette
    private WorldHandle cacheWorldHandle;
    private Long2ByteOpenHashMap cellKey2OpenCount;

    // Window
    private int[] windowProbeOrder;
    private float[] fractionScratch;
    private GlobeFrameStruct frameScratch;

    // Eviction
    private LongOpenHashSet keptKeys;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.cellKey2OpenCount = new Long2ByteOpenHashMap();
        this.cellKey2OpenCount.defaultReturnValue(EngineSetting.OCEAN_EXPOSURE_UNKNOWN);

        // Window
        int span = EngineSetting.OCEAN_EXPOSURE_GRID_SIZE + EngineSetting.OCEAN_EXPOSURE_BLUR_RADIUS_CELLS * 2;
        this.windowProbeOrder = buildProbeOrder();
        this.fractionScratch = new float[span * span];
        this.frameScratch = new GlobeFrameStruct();

        // Eviction
        this.keptKeys = new LongOpenHashSet();

        this.exposureProbeBranch = create(ExposureProbeBranch.class);
    }

    @Override
    protected void get() {
        this.worldStreamManager = get(WorldStreamManager.class);
    }

    // Update \\

    @Override
    protected void lateUpdate() {

        ObjectArrayList<GridInstance> grids = worldStreamManager.getGrids();
        Object[] elements = grids.elements();
        int size = grids.size();
        int budget = EngineSetting.OCEAN_EXPOSURE_PROBE_CELLS_PER_FRAME;
        float response = Math.min(1f, internal.getDeltaTime() * EngineSetting.OCEAN_EXPOSURE_RESPONSE_PER_SECOND);

        for (int i = 0; i < size; i++) {

            GridInstance grid = (GridInstance) elements[i];

            resolveCacheWorld(grid.getWorldHandle());
            placeWindow(grid);
            resolveWindowKeys(grid);
            budget = probeWindow(grid, budget);
            resolveExposure(grid, response);
        }

        if (cellKey2OpenCount.size() > EngineSetting.OCEAN_EXPOSURE_CACHE_MAX_CELLS)
            evictDistantCells(grids);
    }

    private void resolveCacheWorld(WorldHandle worldHandle) {

        if (worldHandle == cacheWorldHandle)
            return;

        cellKey2OpenCount.clear();
        cacheWorldHandle = worldHandle;
    }

    // Window \\

    private void placeWindow(GridInstance grid) {

        long reference = grid.getActiveChunkCoordinate();
        int cellBlocks = EngineSetting.OCEAN_EXPOSURE_CELL_BLOCKS;
        int halfGrid = EngineSetting.OCEAN_EXPOSURE_GRID_SIZE / 2;
        int halfChunk = EngineSetting.CHUNK_SIZE / 2;

        int referenceX = Coordinate2Long.unpackX(reference) * EngineSetting.CHUNK_SIZE;
        int referenceZ = Coordinate2Long.unpackY(reference) * EngineSetting.CHUNK_SIZE;
        int originCellX = Math.floorDiv(referenceX + halfChunk, cellBlocks) - halfGrid;
        int originCellZ = Math.floorDiv(referenceZ + halfChunk, cellBlocks) - halfGrid;

        grid.getExposureInstance().placeWindow(
                originCellX,
                originCellZ,
                originCellX * cellBlocks - referenceX,
                originCellZ * cellBlocks - referenceZ);
    }

    private int probeWindow(GridInstance grid, int budget) {

        long[] cellKeys = grid.getExposureInstance().getCellKeys();
        WorldHandle worldHandle = grid.getWorldHandle();
        int size = EngineSetting.OCEAN_EXPOSURE_GRID_SIZE;
        int radius = EngineSetting.OCEAN_EXPOSURE_BLUR_RADIUS_CELLS;
        int span = size + radius * 2;

        for (int i = 0; i < windowProbeOrder.length; i++) {

            int index = windowProbeOrder[i];
            long cellKey = cellKeys[(index / size + radius) * span + index % size + radius];

            if (cellKey2OpenCount.get(cellKey) != EngineSetting.OCEAN_EXPOSURE_UNKNOWN)
                continue;

            if (budget <= 0)
                return 0;

            cellKey2OpenCount.put(cellKey, exposureProbeBranch.probeOpenWater(
                    worldHandle, Coordinate2Long.unpackX(cellKey), Coordinate2Long.unpackY(cellKey)));
            budget--;
        }

        return budget;
    }

    // Exposure \\

    private void resolveExposure(GridInstance grid, float response) {

        ExposureInstance exposure = grid.getExposureInstance();
        int size = EngineSetting.OCEAN_EXPOSURE_GRID_SIZE;
        int radius = EngineSetting.OCEAN_EXPOSURE_BLUR_RADIUS_CELLS;
        int span = size + radius * 2;

        gatherFractions(grid, exposure, span, radius);

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {

                float fractionSum = 0f;
                int knownCount = 0;

                for (int offsetZ = 0; offsetZ <= radius * 2; offsetZ++) {
                    for (int offsetX = 0; offsetX <= radius * 2; offsetX++) {

                        float fraction = fractionScratch[(z + offsetZ) * span + (x + offsetX)];

                        if (fraction < 0f)
                            continue;

                        fractionSum += fraction;
                        knownCount++;
                    }
                }

                if (knownCount == 0)
                    continue;

                exposure.easeToward(
                        z * size + x,
                        OceanWaveUtility.resolveExposure(fractionSum / knownCount),
                        response);
            }
        }
    }

    private void gatherFractions(GridInstance grid, ExposureInstance exposure, int span, int radius) {

        long[] cellKeys = exposure.getCellKeys();
        float probeCount = EngineSetting.OCEAN_EXPOSURE_PROBES_PER_AXIS * EngineSetting.OCEAN_EXPOSURE_PROBES_PER_AXIS;

        for (int z = 0; z < span; z++) {
            for (int x = 0; x < span; x++) {

                byte openCount = cellKey2OpenCount.get(cellKeys[z * span + x]);

                fractionScratch[z * span + x] = openCount == EngineSetting.OCEAN_EXPOSURE_UNKNOWN
                        ? -1f
                        : openCount / probeCount;
            }
        }
    }

    // Cache \\

    // Every cached cell no grid's window reads any longer is let go
    private void evictDistantCells(ObjectArrayList<GridInstance> grids) {

        keptKeys.clear();

        for (int i = 0; i < grids.size(); i++) {

            long[] cellKeys = grids.get(i).getExposureInstance().getCellKeys();

            for (int k = 0; k < cellKeys.length; k++)
                keptKeys.add(cellKeys[k]);
        }

        ObjectIterator<Long2ByteMap.Entry> iterator = cellKey2OpenCount.long2ByteEntrySet().fastIterator();

        while (iterator.hasNext())
            if (!keptKeys.contains(iterator.next().getLongKey()))
                iterator.remove();
    }

    // Keys \\

    // The cached cell every window cell reads, blur margin included, resolved again whenever the reference moves
    private void resolveWindowKeys(GridInstance grid) {

        ExposureInstance exposure = grid.getExposureInstance();
        long reference = grid.getActiveChunkCoordinate();

        if (exposure.isKeysCurrent(reference))
            return;

        long[] cellKeys = exposure.getCellKeys();
        int radius = EngineSetting.OCEAN_EXPOSURE_BLUR_RADIUS_CELLS;
        int span = EngineSetting.OCEAN_EXPOSURE_GRID_SIZE + radius * 2;

        for (int z = 0; z < span; z++)
            for (int x = 0; x < span; x++)
                cellKeys[z * span + x] = resolveCellKey(
                        grid, exposure.getOriginCellX() + x - radius, exposure.getOriginCellZ() + z - radius);

        exposure.markKeys(reference);
    }

    // A torus cell wrapped into the world, or on a globe the face cell under the window cell's centre
    private long resolveCellKey(GridInstance grid, int cellX, int cellZ) {

        WorldHandle worldHandle = grid.getWorldHandle();

        if (!worldHandle.isGlobe())
            return Coordinate2Long.pack(wrapCellX(worldHandle, cellX), wrapCellZ(worldHandle, cellZ));

        int cellBlocks = EngineSetting.OCEAN_EXPOSURE_CELL_BLOCKS;
        int chunkSize = EngineSetting.CHUNK_SIZE;

        grid.resolveFrame(
                Coordinate2Long.pack(
                        Math.floorDiv(cellX * cellBlocks + cellBlocks / 2, chunkSize),
                        Math.floorDiv(cellZ * cellBlocks + cellBlocks / 2, chunkSize)),
                frameScratch);

        long canonical = frameScratch.getCanonicalCoordinate();

        return Coordinate2Long.pack(
                Math.floorDiv(Coordinate2Long.unpackX(canonical) * chunkSize + chunkSize / 2, cellBlocks),
                Math.floorDiv(Coordinate2Long.unpackY(canonical) * chunkSize + chunkSize / 2, cellBlocks));
    }

    // Utility \\

    private int[] buildProbeOrder() {

        int size = EngineSetting.OCEAN_EXPOSURE_GRID_SIZE;
        float center = (size - 1) * 0.5f;
        long[] keyed = new long[size * size];

        for (int index = 0; index < keyed.length; index++) {

            float dx = index % size - center;
            float dz = index / size - center;

            keyed[index] = ((long) Float.floatToRawIntBits(dx * dx + dz * dz) << 32) | index;
        }

        Arrays.sort(keyed);

        int[] order = new int[keyed.length];

        for (int i = 0; i < keyed.length; i++)
            order[i] = (int) (keyed[i] & EngineSetting.OCEAN_NOISE_UINT_MASK);

        return order;
    }

    private int wrapCellX(WorldHandle worldHandle, int cellX) {
        return Math.floorMod(cellX, getWorldCellsX(worldHandle));
    }

    private int wrapCellZ(WorldHandle worldHandle, int cellZ) {
        return Math.floorMod(cellZ, getWorldCellsZ(worldHandle));
    }

    private int getWorldCellsX(WorldHandle worldHandle) {
        return Math.max(1, worldHandle.getWorldScale().x / EngineSetting.OCEAN_EXPOSURE_CELL_BLOCKS);
    }

    private int getWorldCellsZ(WorldHandle worldHandle) {
        return Math.max(1, worldHandle.getWorldScale().y / EngineSetting.OCEAN_EXPOSURE_CELL_BLOCKS);
    }

    // Grid Factory \\

    public ExposureInstance createExposureInstance() {
        ExposureInstance instance = create(ExposureInstance.class);
        instance.constructor();
        return instance;
    }

    // On-Demand \\

    public float sampleExposure(GridInstance grid, float relativeX, float relativeZ) {
        return grid.getExposureInstance().sample(relativeX, relativeZ);
    }
}
