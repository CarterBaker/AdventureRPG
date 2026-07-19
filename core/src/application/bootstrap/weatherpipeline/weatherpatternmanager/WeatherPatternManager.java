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
     * Simulates the world's persistent weather patterns — jittered cells
     * streamed in across the full radius around the player, each holding a
     * handful of offset cloud lobes that drift with world rotation and
     * retire once they pass beyond the simulated radius. A pattern's lobes
     * rebuild whenever its resolved weather changes, and its bounding
     * geometry is recomputed alongside them — the single source of truth
     * the sky dome preview reads its own box from.
     */

    private static final float FADE_IN_RATE = 0.4f;
    private static final float FADE_OUT_RATE = 0.4f;
    private static final float INTENSITY_SMOOTHING_TIME_SECONDS = 3.0f;
    private static final float MIN_ALTITUDE_HALF_THICKNESS = 4.0f;
    private static final long GENERATION_SEED_MIX = 0x632BE59BD9B4E019L;

    private WeatherManager weatherManager;
    private WorldManager worldManager;
    private SkyWeatherPatternBranch skyWeatherPatternBranch;

    private int patternCellSizeChunks;
    private float radiusChunks;
    private int maxPatternsStreamedPerFrame;
    private int maxActivePatternCount;
    private int overheadLobeBudget;
    private float reevaluationMinSeconds;
    private float reevaluationMaxSeconds;

    private Long2ObjectOpenHashMap<WeatherPatternStruct> activePatterns;
    private int totalActiveLobeCount;

    private IntArrayList freeSlots;

    private ObjectArrayList<int[]> candidateOffsets;
    private int scanCursor;

    private double elapsedSimTime;
    private float intensityUpdateAccumulator;

    private final WeatherBandStruct bandScratch = new WeatherBandStruct();

    private ObjectArrayList<WeatherPatternStruct> streamedInThisFrame;
    private ObjectArrayList<WeatherPatternStruct> retiredThisFrame;
    private ObjectArrayList<WeatherPatternStruct> refreshedThisFrame;

    @Override
    protected void create() {

        this.patternCellSizeChunks = EngineSetting.WEATHER_PATTERN_CELL_SIZE_CHUNKS;
        this.maxPatternsStreamedPerFrame = EngineSetting.OVERHEAD_MAX_STREAM_PER_FRAME;
        this.maxActivePatternCount = EngineSetting.WEATHER_PATTERN_MAX_ACTIVE_COUNT;
        this.overheadLobeBudget = EngineSetting.WEATHER_PATTERN_OVERHEAD_LOBE_BUDGET;
        this.reevaluationMinSeconds = EngineSetting.WEATHER_PATTERN_REEVALUATION_INTERVAL_MIN_SECONDS;
        this.reevaluationMaxSeconds = EngineSetting.WEATHER_PATTERN_REEVALUATION_INTERVAL_MAX_SECONDS;

        this.activePatterns = new Long2ObjectOpenHashMap<>();
        this.totalActiveLobeCount = 0;

        this.freeSlots = new IntArrayList(maxActivePatternCount);
        for (int i = 0; i < maxActivePatternCount; i++)
            freeSlots.add(i);

        this.scanCursor = 0;

        this.elapsedSimTime = 0.0;
        this.intensityUpdateAccumulator = 0f;

        this.streamedInThisFrame = new ObjectArrayList<>();
        this.retiredThisFrame = new ObjectArrayList<>();
        this.refreshedThisFrame = new ObjectArrayList<>();

        this.skyWeatherPatternBranch = create(SkyWeatherPatternBranch.class);
    }

    @Override
    protected void get() {
        this.weatherManager = get(WeatherManager.class);
        this.worldManager = get(WorldManager.class);
    }

    /*
     * radiusChunks reads WeatherManager.getEffectiveNearRangeChunks() — the
     * exact same boundary the sky dome's own near ring is built from —
     * rather than recomputing the range formula locally, so the overhead
     * system's outer edge and the sky's handoff point can never drift out
     * of sync. Deferred to awake() since weatherManager isn't wired until
     * get() has run.
     */
    @Override
    protected void awake() {
        this.radiusChunks = weatherManager.getEffectiveNearRangeChunks();
        this.candidateOffsets = buildCandidateOffsets();
    }

    @Override
    protected void update() {

        streamedInThisFrame.clear();
        retiredThisFrame.clear();
        refreshedThisFrame.clear();

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
     * Every cell within the streaming radius is a spawn candidate, including
     * the one directly under the player — patterns must cover the full disc
     * from the first frame, not just its edge, or nothing is ever overhead
     * until one drifts all the way in from the rim. Sorted upwind-first,
     * then nearest-first, so a budget-constrained frame still prioritizes
     * both the side weather drifts in from and the area right around the
     * player.
     */
    private ObjectArrayList<int[]> buildCandidateOffsets() {

        float jitterRangeChunks = patternCellSizeChunks * EngineSetting.WEATHER_PATTERN_HOME_JITTER_RATIO;
        float maxJitterMagnitudeChunks = (jitterRangeChunks * 0.5f) * (float) Math.sqrt(2.0);
        float candidateRadiusChunks = radiusChunks + maxJitterMagnitudeChunks;
        int radiusCells = Math.max(1, (int) Math.ceil(candidateRadiusChunks / (float) patternCellSizeChunks));

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

        offsets.sort((a, b) -> {
            int upwind = Integer.compare(a[0], b[0]);
            return upwind != 0 ? upwind : Integer.compare(a[2], b[2]);
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

            if (streamInPattern(patternKey, wrappedHomeChunkX, wrappedHomeChunkZ))
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

    private boolean streamInPattern(long patternKey, int homeChunkX, int homeChunkZ) {

        if (freeSlots.isEmpty())
            return false;

        long chunkCoordinate = Coordinate2Long.pack(homeChunkX, homeChunkZ);
        weatherManager.resolveWeatherBandTowardHorizon(bandScratch, chunkCoordinate);

        WeatherHandle weatherHandle = bandScratch.getPrimary();
        float spread = bandScratch.getIntensityFor(weatherHandle);
        float intensity = spread * weatherHandle.getCloudCoverage();

        WeatherPatternLobeStruct[] lobes = buildLobes(patternKey, weatherHandle, 0);

        if (totalActiveLobeCount + lobes.length > overheadLobeBudget)
            return false;

        float driftSpeedScale = computePatternDriftSpeedScale(lobes);
        int slot = freeSlots.removeInt(freeSlots.size() - 1);

        WeatherPatternStruct pattern = new WeatherPatternStruct(
                patternKey, homeChunkX, homeChunkZ, weatherHandle, lobes,
                driftSpeedScale, intensity, spread, slot);

        applyBounds(pattern, lobes);
        pattern.setNextReevaluationTime(elapsedSimTime + reevaluationIntervalFor(patternKey));

        totalActiveLobeCount += lobes.length;
        activePatterns.put(patternKey, pattern);
        streamedInThisFrame.add(pattern);

        return true;
    }

    private WeatherPatternLobeStruct[] buildLobes(long patternKey, WeatherHandle weatherHandle, int generation) {

        int lobeCount = weatherHandle.hasClouds()
                ? Math.max(1, Math.round(lerp(
                        EngineSetting.WEATHER_PATTERN_LOBE_MIN_COUNT,
                        EngineSetting.WEATHER_PATTERN_LOBE_MAX_COUNT,
                        clamp01(weatherHandle.getCloudCoverage()))))
                : 1;

        float lobeSpreadChunks = patternCellSizeChunks * EngineSetting.WEATHER_PATTERN_LOBE_SPREAD_RATIO;
        long generationSalt = GENERATION_SEED_MIX * (generation + 1);

        WeatherPatternLobeStruct[] lobes = new WeatherPatternLobeStruct[lobeCount];

        for (int i = 0; i < lobeCount; i++) {

            long lobeSeedBase = patternKey ^ generationSalt ^ (0x9E3779B97F4A7C15L * (i + 1));

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
            float elongation = lerp(
                    EngineSetting.CLOUD_INSTANCE_ELONGATION_MIN,
                    EngineSetting.CLOUD_INSTANCE_ELONGATION_MAX,
                    hash01(lobeSeedBase ^ 0x27D4EB2F165667C5L));

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
                    randomSeed, sizeVariance, domainRotation, elongation);
        }

        return lobes;
    }

    private void applyBounds(WeatherPatternStruct pattern, WeatherPatternLobeStruct[] lobes) {

        float maxReachChunks = 0f;
        float altMin = Float.MAX_VALUE;
        float altMax = -Float.MAX_VALUE;

        for (int i = 0; i < lobes.length; i++) {

            WeatherPatternLobeStruct lobe = lobes[i];

            if (!lobe.hasCloud())
                continue;

            CloudHandle cloud = lobe.getCloudHandle();
            float footprintBlocks = cloud.getScale() * lobe.getSizeVariance() * Math.max(lobe.getElongation(), 1f);
            float footprintChunks = (footprintBlocks * 0.5f) / EngineSetting.CHUNK_SIZE;
            float offsetChunks = (float) Math.sqrt(
                    lobe.getOffsetChunkX() * lobe.getOffsetChunkX()
                            + lobe.getOffsetChunkZ() * lobe.getOffsetChunkZ());

            maxReachChunks = Math.max(maxReachChunks, offsetChunks + footprintChunks);

            float halfThicknessBlocks = cloud.getVerticalThickness() * lobe.getSizeVariance() * 0.5f;
            float altitude = lobe.getEffectiveAltitude();

            altMin = Math.min(altMin, altitude - halfThicknessBlocks);
            altMax = Math.max(altMax, altitude + halfThicknessBlocks);
        }

        if (altMin > altMax) {
            maxReachChunks = Math.max(maxReachChunks, patternCellSizeChunks * 0.5f);
            altMin = EngineSetting.CLOUD_DEFAULT_SKY_ALTITUDE - MIN_ALTITUDE_HALF_THICKNESS;
            altMax = EngineSetting.CLOUD_DEFAULT_SKY_ALTITUDE + MIN_ALTITUDE_HALF_THICKNESS;
        }

        float halfThickness = Math.max((altMax - altMin) * 0.5f, MIN_ALTITUDE_HALF_THICKNESS);

        pattern.setBounds(maxReachChunks, (altMin + altMax) * 0.5f, halfThickness);
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
                tryRefreshWeather(pattern, resolved);

            pattern.setNextReevaluationTime(elapsedSimTime + reevaluationIntervalFor(pattern.getPatternKey()));
        }
    }

    private void tryRefreshWeather(WeatherPatternStruct pattern, WeatherHandle resolved) {

        WeatherPatternLobeStruct[] newLobes = buildLobes(pattern.getPatternKey(), resolved,
                pattern.getGeneration() + 1);
        int delta = newLobes.length - pattern.getLobeCount();

        if (totalActiveLobeCount + delta > overheadLobeBudget)
            return;

        pattern.beginWeatherTransition(resolved);
        pattern.refreshLobes(newLobes);
        pattern.setDriftSpeedScale(computePatternDriftSpeedScale(newLobes));
        applyBounds(pattern, newLobes);

        totalActiveLobeCount += delta;
        refreshedThisFrame.add(pattern);
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

            float purity = bandScratch.getIntensityFor(pattern.getWeatherHandle());

            pattern.setTargetSpread(purity);
            pattern.setTargetIntensity(purity * pattern.getWeatherHandle().getCloudCoverage());
        }
    }

    private void advanceIntensitySmoothing() {

        float deltaTime = internal.getDeltaTime();
        float alpha = 1f - (float) Math.exp(-deltaTime / INTENSITY_SMOOTHING_TIME_SECONDS);

        for (WeatherPatternStruct pattern : activePatterns.values()) {
            pattern.advanceIntensitySmoothing(alpha);
            pattern.advanceSpreadSmoothing(alpha);
        }
    }

    private float reevaluationIntervalFor(long patternKey) {
        float t = hash01(patternKey ^ 0xD1B54A32D192ED03L);
        return lerp(reevaluationMinSeconds, reevaluationMaxSeconds, t);
    }

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

        totalActiveLobeCount -= pattern.getLobeCount();
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

    public ObjectArrayList<WeatherPatternStruct> getPatternsRefreshedThisFrame() {
        return refreshedThisFrame;
    }

    public int getActivePatternCount() {
        return activePatterns.size();
    }
}