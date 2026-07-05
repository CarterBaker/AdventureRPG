package application.bootstrap.weatherpipeline.overheadmanager;

import application.bootstrap.weatherpipeline.overheadcell.OverheadCellHandle;
import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import application.bootstrap.weatherpipeline.weathermanager.WeatherBandStruct;
import application.bootstrap.weatherpipeline.weathermanager.WeatherManager;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

public class OverheadManager extends ManagerPackage {

    /*
     * Streams OverheadCellHandles on a fixed chunk-space grid (see
     * EngineSetting.OVERHEAD_CELL_SIZE) around WeatherManager's own
     * reference coordinate, out to HORIZON_DISTANCE, batched at
     * OVERHEAD_MAX_STREAM_PER_FRAME cell operations per frame — creates and
     * releases share a single budget, releases spent first, so a large
     * jump in reference position never streams dozens of cells in the same
     * frame.
     *
     * Every candidate cell's distance from the reference is measured with a
     * wrapped, shortest-path signed delta on each axis (wrappedDelta())
     * rather than plain subtraction — the world is a torus, so a cell just
     * past one edge is genuinely adjacent to a reference point just past
     * the opposite edge, and plain subtraction would report it as being on
     * the far side of the whole map instead. A candidate's own grid index
     * is wrapped only afterward, purely to become a stable storage key —
     * the distance math itself always runs in the reference's own
     * unwrapped local frame, since wrapping the index first would corrupt
     * the sign of the very delta that needs to cross the seam correctly.
     *
     * Does nothing until WeatherManager.hasActiveWeatherPool() is true —
     * see that class for why the window before the calendar's first
     * day-tick has no pool to resolve a cell's identity against.
     */

    // Internal
    private WeatherManager weatherManager;
    private WorldManager worldManager;

    // Settings
    private int cellSize;
    private float horizonDistance;
    private int maxStreamPerFrame;

    // Grid
    private Long2ObjectOpenHashMap<OverheadCellHandle> activeCells;

    // Scratch — reused every update(), never reallocated
    private LongOpenHashSet desiredCellKeys;
    private LongArrayList pendingRemoveKeys;
    private LongArrayList pendingAddKeys;
    private WeatherBandStruct bandScratch;

    // Internal \\

    @Override
    protected void create() {

        // Settings
        this.cellSize = EngineSetting.OVERHEAD_CELL_SIZE;
        this.horizonDistance = EngineSetting.HORIZON_DISTANCE;
        this.maxStreamPerFrame = EngineSetting.OVERHEAD_MAX_STREAM_PER_FRAME;

        // Grid
        this.activeCells = new Long2ObjectOpenHashMap<>();

        // Scratch
        this.desiredCellKeys = new LongOpenHashSet();
        this.pendingRemoveKeys = new LongArrayList();
        this.pendingAddKeys = new LongArrayList();
        this.bandScratch = new WeatherBandStruct();
    }

    @Override
    protected void get() {
        this.weatherManager = get(WeatherManager.class);
        this.worldManager = get(WorldManager.class);
    }

    @Override
    protected void update() {

        if (!weatherManager.hasActiveWeatherPool())
            return;

        rebuildDesiredSet();
        queueStreamingChanges();
        applyPendingChanges();
    }

    // Desired Set \\

    private void rebuildDesiredSet() {

        desiredCellKeys.clear();

        long referenceCoordinate = weatherManager.getReferenceCoordinate();
        int referenceChunkX = Coordinate2Long.unpackX(referenceCoordinate);
        int referenceChunkY = Coordinate2Long.unpackY(referenceCoordinate);

        WorldHandle activeWorld = worldManager.getActiveWorld();
        int worldWidthChunks = Math.max(1, activeWorld.getWorldScale().x / EngineSetting.CHUNK_SIZE);
        int worldHeightChunks = Math.max(1, activeWorld.getWorldScale().y / EngineSetting.CHUNK_SIZE);

        int cellsAcrossX = Math.max(1, Math.round(worldWidthChunks / (float) cellSize));
        int cellsAcrossY = Math.max(1, Math.round(worldHeightChunks / (float) cellSize));

        int referenceCellX = Math.floorDiv(referenceChunkX, cellSize);
        int referenceCellY = Math.floorDiv(referenceChunkY, cellSize);

        // +1 cell of margin: a cell's center can sit up to half a cell
        // farther out than a naive horizonDistance/cellSize radius would
        // suggest, since cells are grid-aligned, not centered on the
        // reference point itself.
        int cellRadius = (int) Math.ceil(horizonDistance / cellSize) + 1;

        float horizonDistanceSquared = horizonDistance * horizonDistance;

        for (int dy = -cellRadius; dy <= cellRadius; dy++) {
            for (int dx = -cellRadius; dx <= cellRadius; dx++) {

                int candidateCellX = referenceCellX + dx;
                int candidateCellY = referenceCellY + dy;

                float candidateCenterChunkX = candidateCellX * cellSize + cellSize * 0.5f;
                float candidateCenterChunkY = candidateCellY * cellSize + cellSize * 0.5f;

                float deltaX = wrappedDelta(candidateCenterChunkX - referenceChunkX, worldWidthChunks);
                float deltaY = wrappedDelta(candidateCenterChunkY - referenceChunkY, worldHeightChunks);

                float distanceSquared = deltaX * deltaX + deltaY * deltaY;

                if (distanceSquared > horizonDistanceSquared)
                    continue;

                int wrappedCellX = Math.floorMod(candidateCellX, cellsAcrossX);
                int wrappedCellY = Math.floorMod(candidateCellY, cellsAcrossY);

                desiredCellKeys.add(Coordinate2Long.pack(wrappedCellX, wrappedCellY));
            }
        }
    }

    /*
     * Shortest signed distance between two points on a wrapped axis of the
     * given period — e.g. a point one unit past the world's right edge and
     * a point one unit past its left edge are two units apart on this
     * axis, not (period - 2) units apart, which is what plain subtraction
     * would report. Deltas here are always bounded by roughly
     * HORIZON_DISTANCE, tiny magnitudes, so float is sufficient — this is
     * a different situation from the large-magnitude rotation math in
     * GlobalNoiseBranch, which needed double.
     */
    private float wrappedDelta(float raw, int period) {

        if (period <= 0)
            return raw;

        float halfPeriod = period / 2f;
        float wrapped = raw % period;

        if (wrapped < -halfPeriod)
            wrapped += period;
        else if (wrapped > halfPeriod)
            wrapped -= period;

        return wrapped;
    }

    // Streaming \\

    private void queueStreamingChanges() {

        pendingRemoveKeys.clear();
        pendingAddKeys.clear();

        LongIterator activeIterator = activeCells.keySet().iterator();
        while (activeIterator.hasNext()) {
            long activeKey = activeIterator.nextLong();
            if (!desiredCellKeys.contains(activeKey))
                pendingRemoveKeys.add(activeKey);
        }

        LongIterator desiredIterator = desiredCellKeys.iterator();
        while (desiredIterator.hasNext()) {
            long desiredKey = desiredIterator.nextLong();
            if (!activeCells.containsKey(desiredKey))
                pendingAddKeys.add(desiredKey);
        }
    }

    /*
     * Spends one shared per-frame budget across both releases and creates —
     * releases first, since freeing a cell is pure bookkeeping while
     * creating one resolves weather and picks a cloud. Never applies more
     * than maxStreamPerFrame total operations in a single frame, regardless
     * of how much the desired set changed by.
     */
    private void applyPendingChanges() {

        int budget = maxStreamPerFrame;

        int removeCount = Math.min(budget, pendingRemoveKeys.size());
        for (int i = 0; i < removeCount; i++)
            activeCells.remove(pendingRemoveKeys.getLong(i));

        budget -= removeCount;

        int addCount = Math.min(budget, pendingAddKeys.size());
        for (int i = 0; i < addCount; i++) {
            long key = pendingAddKeys.getLong(i);
            activeCells.put(key, buildCell(key));
        }
    }

    private OverheadCellHandle buildCell(long cellKey) {

        int cellGridX = Coordinate2Long.unpackX(cellKey);
        int cellGridY = Coordinate2Long.unpackY(cellKey);

        int centerChunkX = cellGridX * cellSize + cellSize / 2;
        int centerChunkY = cellGridY * cellSize + cellSize / 2;
        long centerCoordinate = Coordinate2Long.pack(centerChunkX, centerChunkY);

        weatherManager.resolveWeatherBand(bandScratch, centerCoordinate);
        WeatherHandle resolvedWeather = bandScratch.getPrimary();

        float cloudPick = hashToUnitFloat(cellGridX, cellGridY);

        OverheadCellHandle handle = create(OverheadCellHandle.class);
        handle.constructor(cellGridX, cellGridY, resolvedWeather, resolvedWeather.pickCloud(cloudPick));

        return handle;
    }

    /*
     * Deterministic per-cell hash used only to pick which cloud within the
     * resolved weather's own weighted cloud list this cell gets — distinct
     * prime constants from the weather-selection noise hashes in
     * GlobalNoiseBranch/RegionSampleBranch, so cloud choice never quietly
     * correlates with which weather got picked. WeatherHandle's own
     * docstring calls out exactly this case — "callers that need genuine
     * per-instance variety (e.g. each overhead cell) should pick from
     * getCloudEntries() themselves via WeightedChanceUtility" — pickCloud()
     * is that call, wrapped.
     */
    private float hashToUnitFloat(int x, int y) {

        int h = x * 1610612741 + y * 805306457;
        h = (h ^ (h >>> 15)) * 2013265921;

        return ((h ^ (h >>> 13)) & 0x7fffffff) / (float) Integer.MAX_VALUE;
    }

    // Accessible \\

    public int getActiveCellCount() {
        return activeCells.size();
    }
}