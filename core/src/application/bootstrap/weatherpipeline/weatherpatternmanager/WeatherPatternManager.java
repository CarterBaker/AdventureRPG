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
     * a handful of offset cloud lobes. Patterns spawn in an outer ring,
     * drift with the world, and retire once they drift back out past the
     * simulated radius. Weather reassignment crossfades via
     * WeatherPatternStruct's transitionT; resolved intensity is smoothed
     * every frame rather than snapped whenever it's periodically resampled.
     * Re-evaluation biases its pick toward the current weather's own
     * declared next-weather suggestions — a soft nudge, never a guarantee.
     */

    private static final float FADE_IN_RATE = 0.4f;
    private static final float FADE_OUT_RATE = 0.4f;
    private static final float MIN_SPAWN_DISTANCE_RATIO = 0.55f;
    private static final float INTENSITY_SMOOTHING_TIME_SECONDS = 3.0f;

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
        this.radiusChunks = Math.min(settings.maxRenderDistance / 2f, (float) EngineSetting.WEATHER_NEAR_RANGE_CHUNKS);
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
        advanceIntensitySmoothing();
        advanceFadesAndRetire(playerChunkX, playerChunkZ);
        streamInBudgeted(playerChunkX, playerChunkZ);
    }

    /*
     * Candidates are restricted to an outer ring — inside MIN_SPAWN_DISTANCE_RATIO
     * of the radius, nothing is ever allowed to spawn fresh, so weather can
     * never be conjured near the player. Sorted upwind-first (against world
     * drift) so fresh patterns enter from the side weather drifts in from and
     * sweep across rather than appearing arbitrarily; farthest-first is the
     * tiebreak.
     */
    private ObjectArrayList<int[]> buildCandidateOffsets() {

        float candidateRadiusChunks = radiusChunks + patternCellSizeChunks;
        float minSpawnDistanceChunks = radiusChunks * MIN_SPAWN_DISTANCE_RATIO;
        int radiusCells = Math.max(1, Math.round(candidateRadiusChunks / (float) patternCellSizeChunks));

        ObjectArrayList<int[]> offsets = new ObjectArrayList<>();

        for (int ox = -radiusCells; ox <= radiusCells; ox++) {
            for (int oz = -radiusCells; oz <= radiusCells; oz++) {

                float worldOffsetX = ox * patternCellSizeChunks;
                float worldOffsetZ = oz * patternCellSizeChunks;
                float distChunks = (float) Math.sqrt(worldOffsetX * worldOffsetX + worldOffsetZ * worldOffsetZ);

                if (distChunks > candidateRadiusChunks || distChunks < minSpawnDistanceChunks)
                    continue;

                offsets.add(new int[] { ox, oz, Math.round(distChunks) });
            }
        }

        offsets.sort((a, b) -> {
            int upwind = Integer.compare(a[0], b[0]);
            return upwind != 0 ? upwind : Integer.compare(b[2], a[2]);
        });

        return offsets;
    }

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

            boolean coveredByCloud;

            if (!weatherHandle.hasClouds()) {
                coveredByCloud = false;
            } else if (i == 0) {
                coveredByCloud = true;
            } else {
                float coveragePickNoise = hash01(lobeSeedBase ^ 0xA24BAED4963EE407L);
                float presenceChance = lerp(0.55f, 1.0f, clamp01(weatherHandle.getCloudCoverage()));
                coveredByCloud = coveragePickNoise < presenceChance;
            }

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

    private void advanceWorldDrift() {

        float deltaTime = internal.getDeltaTime();
        double baseDeltaChunkX = weatherManager.getWorldDriftChunksPerSecondX() * deltaTime;

        for (WeatherPatternStruct pattern : activePatterns.values()) {
            float patternDriftScale = pattern.getDriftSpeedScale();
            pattern.advanceDrift(baseDeltaChunkX * patternDriftScale, 0.0);
        }
    }

    private void advanceWeatherReevaluation() {

        float deltaTime = internal.getDeltaTime();
        elapsedSimTime += deltaTime;

        for (WeatherPatternStruct pattern : activePatterns.values()) {

            pattern.advanceWeatherTransition(deltaTime);

            if (pattern.isRetiring())
                continue;

            if (elapsedSimTime < pattern.getNextReevaluationTime())
                continue;

            long homeCoordinate = Coordinate2Long.pack(pattern.getHomeChunkX(), pattern.getHomeChunkZ());
            weatherManager.resolveWeatherBandTowardHorizonBiased(bandScratch, homeCoordinate,
                    pattern.getWeatherHandle());

            WeatherHandle resolved = bandScratch.getPrimary();

            if (resolved != pattern.getWeatherHandle())
                pattern.beginWeatherTransition(resolved);

            pattern.setNextReevaluationTime(elapsedSimTime + reevaluationIntervalFor(pattern.getPatternKey()));
        }
    }

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

            pattern.setTargetIntensity(resolvePatternIntensity(bandScratch, pattern.getWeatherHandle()));
        }
    }

    /*
     * Runs every frame regardless of the resample cadence above, easing the
     * displayed intensity toward whatever target the last resample produced
     * instead of snapping to it — this is what removes the visible pop in
     * cloud density/coverage every time intensity is resampled.
     */
    private void advanceIntensitySmoothing() {

        float deltaTime = internal.getDeltaTime();
        float alpha = 1f - (float) Math.exp(-deltaTime / INTENSITY_SMOOTHING_TIME_SECONDS);

        for (WeatherPatternStruct pattern : activePatterns.values())
            pattern.advanceIntensitySmoothing(alpha);
    }

    private float resolvePatternIntensity(WeatherBandStruct band, WeatherHandle handle) {
        return band.getIntensityFor(handle) * handle.getCloudCoverage();
    }

    private float reevaluationIntervalFor(long patternKey) {
        float t = hash01(patternKey ^ 0xD1B54A32D192ED03L);
        return lerp(reevaluationMinSeconds, reevaluationMaxSeconds, t);
    }

    /*
     * Distance is measured against each pattern's own drifted position, not
     * its fixed home — the home never moves, so checking against it would
     * let a pattern drift indefinitely far from the player while still
     * counting as active, permanently starving the pool of free slots.
     */
    private void advanceFadesAndRetire(int playerChunkX, int playerChunkZ) {

        float deltaTime = internal.getDeltaTime();
        LongArrayList toRemove = null;

        WorldHandle activeWorld = worldManager.getActiveWorld();
        int worldWidthChunks = activeWorld.getWorldScale().x / EngineSetting.CHUNK_SIZE;
        int worldHeightChunks = activeWorld.getWorldScale().y / EngineSetting.CHUNK_SIZE;

        for (WeatherPatternStruct pattern : activePatterns.values()) {

            double dx = WorldWrapUtility.wrappedDelta(pattern.getCurrentChunkX(), playerChunkX, worldWidthChunks);
            double dz = WorldWrapUtility.wrappedDelta(pattern.getCurrentChunkZ(), playerChunkZ, worldHeightChunks);
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