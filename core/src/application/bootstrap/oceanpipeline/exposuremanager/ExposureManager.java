package application.bootstrap.oceanpipeline.exposuremanager;

import java.util.Arrays;

import application.bootstrap.oceanpipeline.exposure.ExposureInstance;
import application.bootstrap.oceanpipeline.util.OceanWaveUtility;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
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
     * chunk is final, since every window is placed relative to it.
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

        ExposureInstance exposure = grid.getExposureInstance();
        WorldHandle worldHandle = grid.getWorldHandle();
        int size = EngineSetting.OCEAN_EXPOSURE_GRID_SIZE;

        for (int i = 0; i < windowProbeOrder.length; i++) {

            int index = windowProbeOrder[i];
            int wrappedCellX = wrapCellX(worldHandle, exposure.getOriginCellX() + index % size);
            int wrappedCellZ = wrapCellZ(worldHandle, exposure.getOriginCellZ() + index / size);
            long cellKey = Coordinate2Long.pack(wrappedCellX, wrappedCellZ);

            if (cellKey2OpenCount.get(cellKey) != EngineSetting.OCEAN_EXPOSURE_UNKNOWN)
                continue;

            if (budget <= 0)
                return 0;

            cellKey2OpenCount.put(cellKey, exposureProbeBranch.probeOpenWater(worldHandle, wrappedCellX, wrappedCellZ));
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

        WorldHandle worldHandle = grid.getWorldHandle();
        float probeCount = EngineSetting.OCEAN_EXPOSURE_PROBES_PER_AXIS * EngineSetting.OCEAN_EXPOSURE_PROBES_PER_AXIS;

        for (int z = 0; z < span; z++) {
            for (int x = 0; x < span; x++) {

                long cellKey = Coordinate2Long.pack(
                        wrapCellX(worldHandle, exposure.getOriginCellX() + x - radius),
                        wrapCellZ(worldHandle, exposure.getOriginCellZ() + z - radius));
                byte openCount = cellKey2OpenCount.get(cellKey);

                fractionScratch[z * span + x] = openCount == EngineSetting.OCEAN_EXPOSURE_UNKNOWN
                        ? -1f
                        : openCount / probeCount;
            }
        }
    }

    // Cache \\

    private void evictDistantCells(ObjectArrayList<GridInstance> grids) {

        int keepCells = EngineSetting.OCEAN_EXPOSURE_GRID_SIZE / 2 + EngineSetting.OCEAN_EXPOSURE_BLUR_RADIUS_CELLS + 1;
        int halfGrid = EngineSetting.OCEAN_EXPOSURE_GRID_SIZE / 2;
        ObjectIterator<Long2ByteMap.Entry> iterator = cellKey2OpenCount.long2ByteEntrySet().fastIterator();

        while (iterator.hasNext()) {

            long cellKey = iterator.next().getLongKey();
            int cellX = Coordinate2Long.unpackX(cellKey);
            int cellZ = Coordinate2Long.unpackY(cellKey);
            boolean kept = false;

            for (int i = 0; i < grids.size() && !kept; i++) {

                GridInstance grid = grids.get(i);
                ExposureInstance exposure = grid.getExposureInstance();
                WorldHandle worldHandle = grid.getWorldHandle();

                double deltaX = WorldWrapUtility.wrappedDelta(
                        cellX, exposure.getOriginCellX() + halfGrid, getWorldCellsX(worldHandle));
                double deltaZ = WorldWrapUtility.wrappedDelta(
                        cellZ, exposure.getOriginCellZ() + halfGrid, getWorldCellsZ(worldHandle));

                kept = Math.abs(deltaX) <= keepCells && Math.abs(deltaZ) <= keepCells;
            }

            if (!kept)
                iterator.remove();
        }
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
