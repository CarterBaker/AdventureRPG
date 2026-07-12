// WeatherPatternManager.java
package application.bootstrap.weatherpipeline.weatherpatternmanager;

import application.bootstrap.weatherpipeline.cloud.CloudHandle;
import application.bootstrap.weatherpipeline.weather.CloudChanceStruct;
import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import application.bootstrap.weatherpipeline.weathermanager.WeatherBandStruct;
import application.bootstrap.weatherpipeline.weathermanager.WeatherManager;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WeatherPatternManager extends ManagerPackage {

    /*
     * Simulates the world's persistent weather patterns — up to
     * WEATHER_PATTERN_MAX_ACTIVE_COUNT systems, each a jittered cell holding
     * a handful of offset cloud lobes, streamed in/out around the player and
     * drifted with the world's rotation. Each active pattern owns a stable
     * UBO slot for its whole lifetime (see the free-slot pool below) so
     * SkyWeatherPatternBranch never has to infer array position from map
     * iteration order. OverheadManager and SkyWeatherPatternBranch both read
     * the active set this class maintains.
     */

    private static final float FADE_IN_RATE = 0.6f;
    private static final float FADE_OUT_RATE = 0.6f;

    private WeatherManager weatherManager;
    private WorldManager worldManager;
    private SkyWeatherPatternBranch skyWeatherPatternBranch;

    private int patternCellSizeChunks;
    private float radiusChunks;
    private int maxPatternsStreamedPerFrame;
    private int maxActivePatternCount;
    private float reevaluationMinSeconds;
    private float reevaluationMaxSeconds;

    private Long2ObjectOpenHashMap<WeatherPatternStruct> activePatterns;

    // Free-slot pool — one slot per possible active pattern. Popped when a
    // pattern streams in, pushed back when it's fully retired. Guarantees
    // every pattern keeps the same UBO array index for its entire lifetime.
    private IntArrayList freeSlots;

    private ObjectArrayList<int[]> candidateOffsets;
    private int scanCursor;

    private double elapsedSimTime;
    private float intensityUpdateAccumulator;

    private final WeatherBandStruct bandScratch = new WeatherBandStruct();

    private ObjectArrayList<WeatherPatternStruct> streamedInThisFrame;
    private ObjectArrayList<WeatherPatternStruct> retiredThisFrame;

    @Override
    protected void create() {

        this.patternCellSizeChunks = EngineSetting.WEATHER_PATTERN_CELL_SIZE_CHUNKS;
        this.radiusChunks = Math.min(settings.maxRenderDistance, (float) EngineSetting.WEATHER_NEAR_RANGE_CHUNKS);
        this.maxPatternsStreamedPerFrame = EngineSetting.OVERHEAD_MAX_STREAM_PER_FRAME;
        this.maxActivePatternCount = EngineSetting.WEATHER_PATTERN_MAX_ACTIVE_COUNT;
        this.reevaluationMinSeconds = EngineSetting.WEATHER_PATTERN_REEVALUATION_INTERVAL_MIN_SECONDS;
        this.reevaluationMaxSeconds = EngineSetting.WEATHER_PATTERN_REEVALUATION_INTERVAL_MAX_SECONDS;

        this.activePatterns = new Long2ObjectOpenHashMap<>();

        this.freeSlots = new IntArrayList(maxActivePatternCount);
        for (int i = 0; i < maxActivePatternCount; i++)
            freeSlots.add(i);

        this.candidateOffsets = buildCandidateOffsets();
        this.scanCursor = 0;

        this.elapsedSimTime = 0.0;
        this.intensityUpdateAccumulator = 0f;

        this.streamedInThisFrame = new ObjectArrayList<>();
        this.retiredThisFrame = new ObjectArrayList<>();

        this.skyWeatherPatternBranch = create(SkyWeatherPatternBranch.class);
    }

    @Override
    protected void get() {
        this.weatherManager = get(WeatherManager.class);
        this.worldManager = get(WorldManager.class);
    }

    @Override
    protected void update() {

        streamedInThisFrame.clear();
        retiredThisFrame.clear();

        if (!weatherManager.hasActiveWeatherPool())
            return;

        long referenceCoordinate = weatherManager.getReferenceCoordinate();
        int playerChunkX = Coordinate2Long.unpackX(referenceCoordinate);
        int playerChunkZ = Coordinate2Long.unpackY(referenceCoordinate);

        advanceWorldDrift();
        advanceWeatherReevaluation();
        advanceIntensity();
        advanceFadesAndRetire(playerChunkX, playerChunkZ);
        streamInBudgeted(playerChunkX, playerChunkZ);
    }

    // Candidate Offsets \\

    private ObjectArrayList<int[]> buildCandidateOffsets() {

        // Filtered here using each cell's own corner-aligned offset only —
        // the true home position (cell center plus jitter, see
        // computeHomeJitter) can end up close to a full cell size further
        // out once actually resolved. Scanning one extra cell size of
        // margin guarantees no legitimately in-range cell is excluded;
        // streamInBudgeted() re-checks the exact final distance before ever
        // actually streaming a candidate in, so this radius only controls
        // how many candidates get queued for that precise check.
        float candidateRadiusChunks = radiusChunks + patternCellSizeChunks;
        int radiusCells = Math.max(1, Math.round(candidateRadiusChunks / (float) patternCellSizeChunks));

        ObjectArrayList<int[]> offsets = new ObjectArrayList<>();

        for (int ox = -radiusCells; ox <= radiusCells; ox++) {
            for (int oz = -radiusCells; oz <= radiusCells; oz++) {

                float worldOffsetX = ox * patternCellSizeChunks;
                float worldOffsetZ = oz * patternCellSizeChunks;
                float distChunks = (float) Math.sqrt(worldOffsetX * worldOffsetX + worldOffsetZ * worldOffsetZ);

                if (distChunks > candidateRadiusChunks)
                    continue;

                offsets.add(new int[] { ox, oz, Math.round(distChunks) });
            }
        }

        offsets.sort((a, b) -> Integer.compare(a[2], b[2]));

        return offsets;
    }

    // Streaming \\

    private void streamInBudgeted(int playerChunkX, int playerChunkZ) {

        if (activePatterns.size() >= maxActivePatternCount)
            return;

        int playerCellX = Math.floorDiv(playerChunkX, patternCellSizeChunks);
        int playerCellZ = Math.floorDiv(playerChunkZ, patternCellSizeChunks);

        WorldHandle activeWorld = worldManager.getActiveWorld();
        int worldWidthChunks = activeWorld.getWorldScale().x / EngineSetting.CHUNK_SIZE;
        int worldHeightChunks = activeWorld.getWorldScale().y / EngineSetting.CHUNK_SIZE;

        int streamed = 0;
        int attempts = 0;
        int maxAttempts = candidateOffsets.size();

        while (streamed < maxPatternsStreamedPerFrame
                && activePatterns.size() < maxActivePatternCount
                && attempts < maxAttempts) {

            int[] offset = candidateOffsets.get(scanCursor);
            scanCursor = (scanCursor + 1) % candidateOffsets.size();
            attempts++;

            int cellX = playerCellX + offset[0];
            int cellZ = playerCellZ + offset[1];
            long patternKey = Coordinate2Long.pack(cellX, cellZ);

            if (activePatterns.containsKey(patternKey))
                continue;

            int homeChunkX = cellX * patternCellSizeChunks + patternCellSizeChunks / 2;
            int homeChunkZ = cellZ * patternCellSizeChunks + patternCellSizeChunks / 2;

            int[] jitter = computeHomeJitter(patternKey);
            homeChunkX += jitter[0];
            homeChunkZ += jitter[1];

            // Exact same distance metric advanceFadesAndRetire() checks this
            // pattern's home against later — computed here, before creation,
            // so nothing streams in only to immediately measure as already
            // beyond the retire radius once its jitter is resolved.
            double dx = WorldWrapUtility.wrappedDelta(homeChunkX, playerChunkX, worldWidthChunks);
            double dz = WorldWrapUtility.wrappedDelta(homeChunkZ, playerChunkZ, worldHeightChunks);
            double trueDistanceChunks = Math.sqrt(dx * dx + dz * dz);

            if (trueDistanceChunks > radiusChunks)
                continue;

            long wrappedHome = wrapChunkCoordinate(homeChunkX, homeChunkZ);
            int wrappedHomeChunkX = Coordinate2Long.unpackX(wrappedHome);
            int wrappedHomeChunkZ = Coordinate2Long.unpackY(wrappedHome);

            streamInPattern(patternKey, wrappedHomeChunkX, wrappedHomeChunkZ);
            streamed++;
        }
    }

    private int[] computeHomeJitter(long patternKey) {

        long jitterSeed = patternKey ^ 0x2545F4914F6CDD1DL;

        float jitterTX = hash01(jitterSeed);
        float jitterTZ = hash01(jitterSeed ^ 0x9E3779B97F4A7C15L);

        float jitterRangeChunks = patternCellSizeChunks * EngineSetting.WEATHER_PATTERN_HOME_JITTER_RATIO;

        int jitterX = Math.round((jitterTX - 0.5f) * jitterRangeChunks);
        int jitterZ = Math.round((jitterTZ - 0.5f) * jitterRangeChunks);

        return new int[] { jitterX, jitterZ };
    }

    private void streamInPattern(long patternKey, int homeChunkX, int homeChunkZ) {

        if (freeSlots.isEmpty())
            return;

        long chunkCoordinate = Coordinate2Long.pack(homeChunkX, homeChunkZ);
        weatherManager.resolveWeatherBandTowardHorizon(bandScratch, chunkCoordinate);

        WeatherHandle weatherHandle = bandScratch.getPrimary();
        float intensity = resolvePatternIntensity(bandScratch, weatherHandle);

        int lobeCount = weatherHandle.hasClouds()
                ? Math.max(1, Math.round(lerp(
                        EngineSetting.WEATHER_PATTERN_LOBE_MIN_COUNT,
                        EngineSetting.WEATHER_PATTERN_LOBE_MAX_COUNT,
                        clamp01(weatherHandle.getCloudCoverage()))))
                : 1;

        float lobeSpreadChunks = patternCellSizeChunks * EngineSetting.WEATHER_PATTERN_LOBE_SPREAD_RATIO;

        WeatherPatternLobeStruct[] lobes = new WeatherPatternLobeStruct[lobeCount];

        for (int i = 0; i < lobeCount; i++) {

            long lobeSeedBase = patternKey ^ (0x9E3779B97F4A7C15L * (i + 1));

            float coveragePickNoise = hash01(lobeSeedBase ^ 0xA24BAED4963EE407L);
            boolean coveredByCloud = weatherHandle.hasClouds()
                    && coveragePickNoise < weatherHandle.getCloudCoverage();

            float cloudPickNoise = hash01(lobeSeedBase);
            CloudChanceStruct cloudEntry = coveredByCloud ? weatherHandle.pickCloud(cloudPickNoise) : null;

            float randomSeed = hash01(lobeSeedBase ^ 0xBF58476D1CE4E5B9L);
            float sizeVariance = lerp(
                    EngineSetting.CLOUD_INSTANCE_SIZE_VARIANCE_MIN,
                    EngineSetting.CLOUD_INSTANCE_SIZE_VARIANCE_MAX,
                    hash01(lobeSeedBase ^ 0x94D049BB133111EBL));
            float domainRotation = hash01(lobeSeedBase ^ 0xD1B54A32D192ED03L) * (float) (Math.PI * 2.0);

            float offsetAngle = hash01(lobeSeedBase ^ 0xC2B2AE3D27D4EB4FL) * (float) (Math.PI * 2.0);
            float offsetRadiusT = i == 0 ? 0f : hash01(lobeSeedBase ^ 0x165667B19E3779F9L);
            float offsetRadius = offsetRadiusT * lobeSpreadChunks;

            float offsetChunkX = (float) Math.cos(offsetAngle) * offsetRadius;
            float offsetChunkZ = (float) Math.sin(offsetAngle) * offsetRadius;

            CloudHandle cloudHandle = cloudEntry != null ? cloudEntry.getCloudHandle() : null;
            float effectiveAltitude = cloudEntry != null
                    ? cloudEntry.getEffectiveAltitude()
                    : EngineSetting.CLOUD_DEFAULT_SKY_ALTITUDE;

            lobes[i] = new WeatherPatternLobeStruct(
                    offsetChunkX, offsetChunkZ,
                    cloudHandle, effectiveAltitude,
                    randomSeed, sizeVariance, domainRotation);
        }

        float driftSpeedScale = computePatternDriftSpeedScale(lobes);
        int slot = freeSlots.removeInt(freeSlots.size() - 1);

        WeatherPatternStruct pattern = new WeatherPatternStruct(
                patternKey, homeChunkX, homeChunkZ, weatherHandle, lobes, driftSpeedScale, intensity, slot);

        pattern.setNextReevaluationTime(elapsedSimTime + reevaluationIntervalFor(patternKey));

        activePatterns.put(patternKey, pattern);
        streamedInThisFrame.add(pattern);
    }

    private float computePatternDriftSpeedScale(WeatherPatternLobeStruct[] lobes) {

        float sum = 0f;
        int count = 0;

        for (int i = 0; i < lobes.length; i++) {
            if (lobes[i].hasCloud()) {
                sum += lobes[i].getCloudHandle().getDriftSpeedScale();
                count++;
            }
        }

        return count > 0 ? sum / count : 1f;
    }

    private long wrapChunkCoordinate(int chunkX, int chunkZ) {

        WorldHandle activeWorld = worldManager.getActiveWorld();
        int worldWidthChunks = activeWorld.getWorldScale().x / EngineSetting.CHUNK_SIZE;
        int worldHeightChunks = activeWorld.getWorldScale().y / EngineSetting.CHUNK_SIZE;

        int wrappedX = Math.floorMod(chunkX, worldWidthChunks);
        int wrappedZ = Math.floorMod(chunkZ, worldHeightChunks);

        return Coordinate2Long.pack(wrappedX, wrappedZ);
    }

    // World Drift \\

    private void advanceWorldDrift() {

        float deltaTime = internal.getDeltaTime();
        double baseDeltaChunkX = weatherManager.getWorldDriftChunksPerSecondX() * deltaTime;

        for (WeatherPatternStruct pattern : activePatterns.values()) {
            float patternDriftScale = pattern.getDriftSpeedScale();
            pattern.advanceDrift(baseDeltaChunkX * patternDriftScale, 0.0);
        }
    }

    // Weather Reevaluation \\

    private void advanceWeatherReevaluation() {

        elapsedSimTime += internal.getDeltaTime();

        for (WeatherPatternStruct pattern : activePatterns.values()) {

            if (pattern.isRetiring())
                continue;

            if (elapsedSimTime < pattern.getNextReevaluationTime())
                continue;

            long homeCoordinate = Coordinate2Long.pack(pattern.getHomeChunkX(), pattern.getHomeChunkZ());
            weatherManager.resolveWeatherBandTowardHorizon(bandScratch, homeCoordinate);

            if (bandScratch.getPrimary() != pattern.getWeatherHandle())
                pattern.setRetiring(true);
            else
                pattern.setNextReevaluationTime(elapsedSimTime + reevaluationIntervalFor(pattern.getPatternKey()));
        }
    }

    // Intensity \\

    private void advanceIntensity() {

        intensityUpdateAccumulator += internal.getDeltaTime();

        if (intensityUpdateAccumulator < EngineSetting.WEATHER_PATTERN_INTENSITY_UPDATE_INTERVAL_SECONDS)
            return;

        intensityUpdateAccumulator = 0f;

        for (WeatherPatternStruct pattern : activePatterns.values()) {

            if (pattern.isRetiring())
                continue;

            long homeCoordinate = Coordinate2Long.pack(pattern.getHomeChunkX(), pattern.getHomeChunkZ());
            weatherManager.resolveWeatherBandTowardHorizon(bandScratch, homeCoordinate);

            pattern.setIntensity(resolvePatternIntensity(bandScratch, pattern.getWeatherHandle()));
        }
    }

    private float resolvePatternIntensity(WeatherBandStruct band, WeatherHandle handle) {
        return band.getIntensityFor(handle) * handle.getCloudCoverage();
    }

    private float reevaluationIntervalFor(long patternKey) {
        float t = hash01(patternKey ^ 0xD1B54A32D192ED03L);
        return lerp(reevaluationMinSeconds, reevaluationMaxSeconds, t);
    }

    // Fade / Retire \\

    private void advanceFadesAndRetire(int playerChunkX, int playerChunkZ) {

        float deltaTime = internal.getDeltaTime();
        LongArrayList toRemove = null;

        WorldHandle activeWorld = worldManager.getActiveWorld();
        int worldWidthChunks = activeWorld.getWorldScale().x / EngineSetting.CHUNK_SIZE;
        int worldHeightChunks = activeWorld.getWorldScale().y / EngineSetting.CHUNK_SIZE;

        for (WeatherPatternStruct pattern : activePatterns.values()) {

            double dx = WorldWrapUtility.wrappedDelta(pattern.getHomeChunkX(), playerChunkX, worldWidthChunks);
            double dz = WorldWrapUtility.wrappedDelta(pattern.getHomeChunkZ(), playerChunkZ, worldHeightChunks);
            double distChunks = Math.sqrt(dx * dx + dz * dz);

            if (distChunks > radiusChunks && !pattern.isRetiring())
                pattern.setRetiring(true);

            float alpha = pattern.getFadeAlpha();

            if (pattern.isRetiring()) {

                alpha = Math.max(0f, alpha - FADE_OUT_RATE * deltaTime);
                pattern.setFadeAlpha(alpha);

                if (alpha <= 0f) {
                    if (toRemove == null)
                        toRemove = new LongArrayList();
                    toRemove.add(pattern.getPatternKey());
                }

            } else if (alpha < 1f) {
                pattern.setFadeAlpha(Math.min(1f, alpha + FADE_IN_RATE * deltaTime));
            }
        }

        if (toRemove != null)
            for (int i = 0; i < toRemove.size(); i++)
                removePattern(toRemove.getLong(i));
    }

    private void removePattern(long patternKey) {

        WeatherPatternStruct pattern = activePatterns.remove(patternKey);

        if (pattern == null)
            return;

        freeSlots.add(pattern.getSlot());
        retiredThisFrame.add(pattern);
    }

    // Noise \\

    private static float hash01(long seed) {

        long h = seed;
        h ^= (h >>> 33);
        h *= 0xff51afd7ed558ccdL;
        h ^= (h >>> 33);
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= (h >>> 33);

        return (float) ((h >>> 11) / (double) (1L << 53));
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    // Accessible \\

    public Long2ObjectOpenHashMap<WeatherPatternStruct> getActivePatterns() {
        return activePatterns;
    }

    public ObjectArrayList<WeatherPatternStruct> getPatternsStreamedInThisFrame() {
        return streamedInThisFrame;
    }

    public ObjectArrayList<WeatherPatternStruct> getPatternsRetiredThisFrame() {
        return retiredThisFrame;
    }

    public int getActivePatternCount() {
        return activePatterns.size();
    }
}