package application.bootstrap.weatherpipeline.overheadmanager;

import application.bootstrap.weatherpipeline.cloud.CloudHandle;
import application.bootstrap.weatherpipeline.weather.CloudChanceStruct;
import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import application.bootstrap.weatherpipeline.weathermanager.WeatherBandStruct;
import application.bootstrap.weatherpipeline.weathermanager.WeatherManager;
import application.bootstrap.weatherpipeline.windmanager.WindManager;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class OverheadManager extends ManagerPackage {

    /*
     * Streams a grid of persistent-identity "overhead cells" around the
     * weather system's current reference coordinate (see
     * WeatherManager.updateReferenceCoordinate()) — the CPU-side source of
     * truth Stage 2/3's instanced cloud renderer reads from. Each cell owns
     * one fixed WeatherHandle + CloudHandle choice for its entire lifetime,
     * resolved once at stream-in via WeatherManager.resolveWeatherBand()
     * .getPrimary(), which is exactly the "persistent, non-reblending
     * identity" RegionSampleBranch's own doc comment describes as this
     * system's reason for existing — unlike the continuously-reblending
     * 9-point horizon sampling, an overhead cloud cell must not re-roll its
     * own identity every frame or every cloud in the sky would flicker
     * between types constantly.
     *
     * A cell whose resolved weather defines no clouds at all (see
     * WeatherHandle.hasClouds()) — a Clear weather — still streams in and
     * holds its WeatherHandle identity exactly like any other cell; only
     * its CloudHandle is null. That is deliberate: the weather itself is
     * still "active" at that patch of sky even though nothing is drawn
     * there, and keeping the cell (rather than skipping the slot entirely)
     * is what stops a clear patch from being re-attempted every single
     * scan cycle. CloudRenderSystem skips these cells outright when
     * rebuilding its instance buffers — see OverheadCellStruct.hasCloud().
     *
     * Streaming is split into two passes, mirroring ChunkQueueManager's own
     * cheap-scan-vs-budgeted-work split:
     *
     * - Every frame, ALL active cells get a cheap pass: advance their wind
     * drift, check whether they've drifted out of the streaming radius,
     * and advance their fade-in/fade-out alpha. This is plain float math —
     * with a few hundred to ~1000 active cells it costs nothing, so it is
     * never budgeted.
     *
     * - Streaming a genuinely NEW cell in (resolving its weather band,
     * picking its cloud, allocating its struct) is capped at
     * EngineSetting.OVERHEAD_MAX_STREAM_PER_FRAME per frame, walked across
     * a precomputed, distance-sorted list of cell offsets relative to
     * whichever cell the reference coordinate currently sits in — the same
     * "offsets relative to a recentering origin" trick the world's own
     * chunk grid uses, just at a coarser cell size and driven by weather
     * instead of terrain streaming.
     *
     * A cell's rendered position is its fixed home center plus accumulated
     * wind drift — never re-centered or wrapped mid-life. Retirement is
     * checked against the cell's home distance from the player, not its
     * drifted position, so a strong gust can't retroactively "un-retire" a
     * cell that's already fading out. Cells whose home chunk falls outside
     * the active world's own bounds (derived from the world PNG's pixel
     * dimensions — see WorldBuilder) are simply never streamed in; this is
     * a bounded world, not a wrapping one, for cloud placement purposes.
     *
     * Streaming radius is EngineSetting.WEATHER_NEAR_RANGE_CHUNKS — the
     * near side of the near/far range pair (see EngineSetting's own doc
     * comment) — deliberately smaller than RegionSampleBranch's far-range
     * 8-direction sample distance, so real cloud geometry only ever exists
     * inside the range the sky-dome preview has already been showing.
     */

    // Fade rates — alpha units per second
    private static final float FADE_IN_RATE = 0.6f;
    private static final float FADE_OUT_RATE = 0.6f;

    // Internal
    private WeatherManager weatherManager;
    private WorldManager worldManager;
    private WindManager windManager;

    // Settings
    private int cellSizeChunks;
    private float radiusChunks;
    private int maxStreamPerFrame;
    private float driftScale;

    // Registry
    private Long2ObjectOpenHashMap<OverheadCellStruct> activeCells;

    // Streaming — distance-sorted offsets (in cells) relative to the
    // reference cell, walked round-robin across frames.
    private ObjectArrayList<int[]> candidateOffsets;
    private int scanCursor;

    // Scratch — reused every stream-in call, never reallocated
    private final WeatherBandStruct bandScratch = new WeatherBandStruct();

    // Internal \\

    @Override
    protected void create() {

        // Settings
        this.cellSizeChunks = EngineSetting.OVERHEAD_CELL_SIZE;
        this.radiusChunks = EngineSetting.WEATHER_NEAR_RANGE_CHUNKS;
        this.maxStreamPerFrame = EngineSetting.OVERHEAD_MAX_STREAM_PER_FRAME;
        this.driftScale = EngineSetting.OVERHEAD_DRIFT_SPEED_SCALE;

        // Registry
        this.activeCells = new Long2ObjectOpenHashMap<>();

        // Streaming
        this.candidateOffsets = buildCandidateOffsets();
        this.scanCursor = 0;
    }

    @Override
    protected void get() {
        this.weatherManager = get(WeatherManager.class);
        this.worldManager = get(WorldManager.class);
        this.windManager = get(WindManager.class);
    }

    @Override
    protected void update() {

        if (!weatherManager.hasActiveWeatherPool())
            return;

        long referenceCoordinate = weatherManager.getReferenceCoordinate();
        int playerChunkX = Coordinate2Long.unpackX(referenceCoordinate);
        int playerChunkZ = Coordinate2Long.unpackY(referenceCoordinate);

        int playerCellX = Math.floorDiv(playerChunkX, cellSizeChunks);
        int playerCellZ = Math.floorDiv(playerChunkZ, cellSizeChunks);

        advanceWindDrift();
        advanceFadesAndRetire(playerChunkX, playerChunkZ);
        streamInBudgeted(playerCellX, playerCellZ);
    }

    // Candidate Offsets \\

    /*
     * Builds every (offsetX, offsetZ) cell offset — in cell units, relative
     * to whichever cell the reference coordinate currently occupies — whose
     * center falls within radiusChunks, sorted nearest-first so newly
     * streamed cells always fill in from the player outward rather than in
     * scan order. A one-time cost at create() — a few hundred to ~1000
     * entries even at default settings, trivial to sort once.
     */
    private ObjectArrayList<int[]> buildCandidateOffsets() {

        int radiusCells = Math.max(1, Math.round(radiusChunks / (float) cellSizeChunks));

        ObjectArrayList<int[]> offsets = new ObjectArrayList<>();

        for (int ox = -radiusCells; ox <= radiusCells; ox++) {
            for (int oz = -radiusCells; oz <= radiusCells; oz++) {

                float worldOffsetX = ox * cellSizeChunks;
                float worldOffsetZ = oz * cellSizeChunks;
                float distChunks = (float) Math.sqrt(
                        worldOffsetX * worldOffsetX + worldOffsetZ * worldOffsetZ);

                if (distChunks > radiusChunks)
                    continue;

                offsets.add(new int[] { ox, oz, Math.round(distChunks) });
            }
        }

        offsets.sort((a, b) -> Integer.compare(a[2], b[2]));

        return offsets;
    }

    // Streaming \\

    private void streamInBudgeted(int playerCellX, int playerCellZ) {

        int streamed = 0;
        int attempts = 0;
        int maxAttempts = candidateOffsets.size();

        while (streamed < maxStreamPerFrame && attempts < maxAttempts) {

            int[] offset = candidateOffsets.get(scanCursor);
            scanCursor = (scanCursor + 1) % candidateOffsets.size();
            attempts++;

            int cellX = playerCellX + offset[0];
            int cellZ = playerCellZ + offset[1];
            long cellKey = Coordinate2Long.pack(cellX, cellZ);

            if (activeCells.containsKey(cellKey))
                continue;

            int homeChunkX = cellX * cellSizeChunks + cellSizeChunks / 2;
            int homeChunkZ = cellZ * cellSizeChunks + cellSizeChunks / 2;

            if (!isWithinWorldBounds(homeChunkX, homeChunkZ))
                continue;

            streamInCell(cellKey, homeChunkX, homeChunkZ);
            streamed++;
        }
    }

    private void streamInCell(long cellKey, int homeChunkX, int homeChunkZ) {

        long chunkCoordinate = Coordinate2Long.pack(homeChunkX, homeChunkZ);
        weatherManager.resolveWeatherBand(bandScratch, chunkCoordinate);

        WeatherHandle weatherHandle = bandScratch.getPrimary();
        float cloudPickNoise = hash01(cellKey);
        CloudChanceStruct cloudEntry = weatherHandle.pickCloud(cloudPickNoise);
        float randomSeed = hash01(cellKey ^ 0x9E3779B97F4A7C15L);

        // A Clear (or otherwise cloudless) weather resolves to a null
        // cloud entry — the cell still streams in and carries its
        // WeatherHandle identity, it simply has nothing to draw. See the
        // class doc comment.
        CloudHandle cloudHandle = cloudEntry != null ? cloudEntry.getCloudHandle() : null;
        float effectiveAltitude = cloudEntry != null
                ? cloudEntry.getEffectiveAltitude()
                : EngineSetting.CLOUD_DEFAULT_SKY_ALTITUDE;

        OverheadCellStruct cell = new OverheadCellStruct(
                cellKey,
                homeChunkX, homeChunkZ,
                weatherHandle,
                cloudHandle,
                effectiveAltitude,
                randomSeed);

        activeCells.put(cellKey, cell);
    }

    private boolean isWithinWorldBounds(int chunkX, int chunkZ) {

        WorldHandle activeWorld = worldManager.getActiveWorld();
        int worldWidthChunks = activeWorld.getWorldScale().x / EngineSetting.CHUNK_SIZE;
        int worldHeightChunks = activeWorld.getWorldScale().y / EngineSetting.CHUNK_SIZE;

        return chunkX >= 0 && chunkX < worldWidthChunks
                && chunkZ >= 0 && chunkZ < worldHeightChunks;
    }

    // Wind Drift \\

    /*
     * Advances every active cell's drifted position by the same live local
     * wind vector every other wind-aware system reads — see
     * LocalWindBranch and RegionSampleBranch's own identical pattern.
     * driftScale (EngineSetting.OVERHEAD_DRIFT_SPEED_SCALE) is the overhead
     * grid's own chunks-per-second-per-unit-of-wind-speed constant, kept
     * separate from RegionSampleBranch's WEATHER_WIND_DRIFT_SCALE so the
     * two can be tuned independently (visible cloud objects drifting at a
     * different apparent rate than the CPU noise field driving them is
     * fine — they're not required to stay pixel-locked to each other).
     */
    private void advanceWindDrift() {

        Vector3 windDirection = windManager.getWindHandle().getLocalWindDirection();
        float windSpeed = windManager.getWindHandle().getLocalWindSpeed();
        float deltaTime = internal.getDeltaTime();

        double deltaChunkX = windDirection.x * windSpeed * driftScale * deltaTime;
        double deltaChunkZ = windDirection.z * windSpeed * driftScale * deltaTime;

        for (OverheadCellStruct cell : activeCells.values())
            cell.advanceDrift(deltaChunkX, deltaChunkZ);
    }

    // Fade / Retire \\

    private void advanceFadesAndRetire(int playerChunkX, int playerChunkZ) {

        float deltaTime = internal.getDeltaTime();
        LongArrayList toRemove = null;

        for (OverheadCellStruct cell : activeCells.values()) {

            double dx = cell.getHomeChunkX() - playerChunkX;
            double dz = cell.getHomeChunkZ() - playerChunkZ;
            double distChunks = Math.sqrt(dx * dx + dz * dz);

            if (distChunks > radiusChunks && !cell.isRetiring())
                cell.setRetiring(true);

            float alpha = cell.getFadeAlpha();

            if (cell.isRetiring()) {

                alpha = Math.max(0f, alpha - FADE_OUT_RATE * deltaTime);
                cell.setFadeAlpha(alpha);

                if (alpha <= 0f) {
                    if (toRemove == null)
                        toRemove = new LongArrayList();
                    toRemove.add(cell.getCellKey());
                }

            } else if (alpha < 1f) {
                cell.setFadeAlpha(Math.min(1f, alpha + FADE_IN_RATE * deltaTime));
            }
        }

        if (toRemove != null)
            for (int i = 0; i < toRemove.size(); i++)
                activeCells.remove(toRemove.getLong(i));
    }

    // Noise \\

    /*
     * Deterministic finalizer-style mix — same shape as
     * DayTrackerBranch.calculateRandomNoise() — turning a cell's stable
     * packed coordinate key into a uniform [0, 1) float. Never re-rolled
     * for a given cell's lifetime, which is exactly what makes a cell's
     * cloud choice and shape seed "persistent" rather than reblending.
     */
    private static float hash01(long seed) {

        long h = seed;
        h ^= (h >>> 33);
        h *= 0xff51afd7ed558ccdL;
        h ^= (h >>> 33);
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= (h >>> 33);

        return (float) ((h >>> 11) / (double) (1L << 53));
    }

    // Accessible \\

    /*
     * Live registry of active overhead cells, keyed by packed cell
     * coordinate. Stage 2's instanced cloud renderer reads this directly
     * each frame to keep its per-cloud-type instance buffers in sync —
     * treat as read-only; all mutation happens here.
     */
    public Long2ObjectOpenHashMap<OverheadCellStruct> getActiveCells() {
        return activeCells;
    }

    public int getActiveCellCount() {
        return activeCells.size();
    }
}