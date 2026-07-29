package application.bootstrap.weatherpipeline.weatherpatternmanager;

import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import application.bootstrap.weatherpipeline.weathermanager.WeatherBandStruct;
import application.bootstrap.weatherpipeline.weathermanager.WeatherManager;
import application.bootstrap.weatherpipeline.weatherpattern.WeatherPatternInstance;
import application.bootstrap.worldpipeline.grid.GridInstance;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import application.bootstrap.worldpipeline.worldstreammanager.WorldStreamManager;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WeatherPatternManager extends ManagerPackage {

    /*
     * Streams jittered spatial cells around every active grid into one shared,
     * pool-recycled pattern set keyed by world cell, so any two grids sharing
     * a cell see the exact same weather. Each grid also owns its own
     * WeatherPatternInstance (held directly on GridInstance) resolved against
     * that grid's own reference coordinate, driving local temperature and wind.
     */

    private static final float DEFAULT_DRIFT_SPEED_SCALE = 1.0f;
    private static final long LOCAL_PATTERN_KEY_SEED = Long.MIN_VALUE;

    private WeatherManager weatherManager;
    private WorldManager worldManager;
    private WorldStreamManager worldStreamManager;
    private TemperatureSystem temperatureSystem;

    private int patternCellSizeChunks;
    private float outerRangeChunks;
    private float nearRangeChunks;
    private int maxPatternsStreamedPerFrame;
    private int maxActivePatternCount;
    private float reevaluationNoiseFraction;
    private float reevaluationJitterMin;
    private float reevaluationJitterMax;
    private float reevaluationMinSeconds;
    private float reevaluationMaxSeconds;
    private float intensitySmoothingTimeSeconds;
    private float fadeInRate;
    private float fadeOutRate;

    private Long2ObjectOpenHashMap<WeatherPatternInstance> activePatterns;
    private Object2ObjectOpenHashMap<GridInstance, Float> gridToTemperature;

    private WeatherPatternInstance[] patternPool;
    private boolean[] slotActive;
    private IntArrayList freeSlots;
    private IntArrayList pendingFreeSlots;

    private ObjectArrayList<int[]> candidateOffsets;
    private int scanCursor;

    private double elapsedSimTime;
    private float intensityUpdateAccumulator;

    private final WeatherBandStruct bandScratch = new WeatherBandStruct();
    private final int[] homeJitterScratch = new int[2];

    private ObjectArrayList<WeatherPatternInstance> streamedInThisFrame;
    private ObjectArrayList<WeatherPatternInstance> retiredThisFrame;
    private ObjectArrayList<WeatherPatternInstance> refreshedThisFrame;

    @Override
    protected void create() {

        this.patternCellSizeChunks = EngineSetting.WEATHER_PATTERN_CELL_SIZE_CHUNKS;
        this.maxPatternsStreamedPerFrame = EngineSetting.OVERHEAD_MAX_STREAM_PER_FRAME;
        this.maxActivePatternCount = EngineSetting.WEATHER_PATTERN_MAX_ACTIVE_COUNT;
        this.reevaluationNoiseFraction = EngineSetting.WEATHER_PATTERN_REEVALUATION_NOISE_FRACTION;
        this.reevaluationJitterMin = EngineSetting.WEATHER_PATTERN_REEVALUATION_JITTER_MIN;
        this.reevaluationJitterMax = EngineSetting.WEATHER_PATTERN_REEVALUATION_JITTER_MAX;
        this.reevaluationMinSeconds = EngineSetting.WEATHER_PATTERN_REEVALUATION_MIN_SECONDS;
        this.reevaluationMaxSeconds = EngineSetting.WEATHER_PATTERN_REEVALUATION_MAX_SECONDS;
        this.intensitySmoothingTimeSeconds = EngineSetting.WEATHER_PATTERN_INTENSITY_SMOOTHING_TIME_SECONDS;
        this.fadeInRate = EngineSetting.WEATHER_PATTERN_FADE_IN_RATE;
        this.fadeOutRate = EngineSetting.WEATHER_PATTERN_FADE_OUT_RATE;

        this.activePatterns = new Long2ObjectOpenHashMap<>();
        this.gridToTemperature = new Object2ObjectOpenHashMap<>();

        this.freeSlots = new IntArrayList(maxActivePatternCount);
        this.pendingFreeSlots = new IntArrayList(maxActivePatternCount);
        this.patternPool = new WeatherPatternInstance[maxActivePatternCount];
        this.slotActive = new boolean[maxActivePatternCount];

        for (int i = 0; i < maxActivePatternCount; i++) {
            freeSlots.add(i);
            WeatherPatternInstance pattern = create(WeatherPatternInstance.class);
            pattern.assignSlot(i);
            patternPool[i] = pattern;
        }

        this.scanCursor = 0;

        this.elapsedSimTime = 0.0;
        this.intensityUpdateAccumulator = 0f;

        this.streamedInThisFrame = new ObjectArrayList<>();
        this.retiredThisFrame = new ObjectArrayList<>();
        this.refreshedThisFrame = new ObjectArrayList<>();

        this.temperatureSystem = create(TemperatureSystem.class);
        create(WeatherMapBufferSystem.class);
    }

    @Override
    protected void get() {
        this.weatherManager = get(WeatherManager.class);
        this.worldManager = get(WorldManager.class);
        this.worldStreamManager = get(WorldStreamManager.class);
    }

    @Override
    protected void awake() {
        this.outerRangeChunks = weatherManager.getEffectiveOuterRangeChunks();
        this.nearRangeChunks = weatherManager.getEffectiveNearRangeChunks();
        this.candidateOffsets = buildCandidateOffsets();
    }

    @Override
    protected void update() {

        streamedInThisFrame.clear();
        retiredThisFrame.clear();
        refreshedThisFrame.clear();

        if (!pendingFreeSlots.isEmpty()) {
            freeSlots.addAll(pendingFreeSlots);
            pendingFreeSlots.clear();
        }

        if (!weatherManager.hasActiveWeatherPool()) {
            gridToTemperature.clear();
            return;
        }

        ObjectArrayList<GridInstance> grids = worldStreamManager.getGrids();

        advanceWorldDrift();
        advanceWeatherReevaluation(grids);
        advanceLocalWeather(grids);
        advanceIntensity(grids);
        advanceIntensitySmoothing();
        advanceFadesAndRetire(grids);
        streamInBudgeted(grids);
    }

    // Candidate Offsets \\

    private ObjectArrayList<int[]> buildCandidateOffsets() {

        float jitterRangeChunks = patternCellSizeChunks * EngineSetting.WEATHER_PATTERN_HOME_JITTER_RATIO;
        float maxJitterMagnitudeChunks = (jitterRangeChunks * 0.5f) * (float) Math.sqrt(2.0);
        float candidateRadiusChunks = outerRangeChunks + maxJitterMagnitudeChunks;
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

    // Streaming \\

    private void streamInBudgeted(ObjectArrayList<GridInstance> grids) {

        if (activePatterns.size() >= maxActivePatternCount)
            return;

        Object[] elements = grids.elements();
        int gridCount = grids.size();
        int streamedTotal = 0;

        for (int g = 0; g < gridCount
                && streamedTotal < maxPatternsStreamedPerFrame
                && activePatterns.size() < maxActivePatternCount; g++) {

            long referenceCoordinate = ((GridInstance) elements[g]).getActiveChunkCoordinate();
            int playerChunkX = Coordinate2Long.unpackX(referenceCoordinate);
            int playerChunkZ = Coordinate2Long.unpackY(referenceCoordinate);

            streamedTotal += streamInForReference(
                    playerChunkX, playerChunkZ, maxPatternsStreamedPerFrame - streamedTotal);
        }
    }

    private int streamInForReference(int playerChunkX, int playerChunkZ, int budget) {

        int playerCellX = Math.floorDiv(playerChunkX, patternCellSizeChunks);
        int playerCellZ = Math.floorDiv(playerChunkZ, patternCellSizeChunks);

        WorldHandle activeWorld = worldManager.getActiveWorld();
        int worldWidthChunks = activeWorld.getWorldScale().x / EngineSetting.CHUNK_SIZE;
        int worldHeightChunks = activeWorld.getWorldScale().y / EngineSetting.CHUNK_SIZE;

        int streamed = 0;
        int attempts = 0;
        int maxAttempts = candidateOffsets.size();

        while (streamed < budget
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

            computeHomeJitter(patternKey);
            homeChunkX += homeJitterScratch[0];
            homeChunkZ += homeJitterScratch[1];

            double dx = WorldWrapUtility.wrappedDelta(homeChunkX, playerChunkX, worldWidthChunks);
            double dz = WorldWrapUtility.wrappedDelta(homeChunkZ, playerChunkZ, worldHeightChunks);
            double trueDistanceChunks = Math.sqrt(dx * dx + dz * dz);

            if (trueDistanceChunks > outerRangeChunks)
                continue;

            long wrappedHome = wrapChunkCoordinate(homeChunkX, homeChunkZ);
            int wrappedHomeChunkX = Coordinate2Long.unpackX(wrappedHome);
            int wrappedHomeChunkZ = Coordinate2Long.unpackY(wrappedHome);

            if (streamInPattern(patternKey, wrappedHomeChunkX, wrappedHomeChunkZ, trueDistanceChunks,
                    playerChunkX, playerChunkZ))
                streamed++;
        }

        return streamed;
    }

    private void computeHomeJitter(long patternKey) {

        long jitterSeed = patternKey ^ 0x2545F4914F6CDD1DL;

        float jitterTX = hash01(jitterSeed);
        float jitterTZ = hash01(jitterSeed ^ 0x9E3779B97F4A7C15L);

        float jitterRangeChunks = patternCellSizeChunks * EngineSetting.WEATHER_PATTERN_HOME_JITTER_RATIO;

        homeJitterScratch[0] = Math.round((jitterTX - 0.5f) * jitterRangeChunks);
        homeJitterScratch[1] = Math.round((jitterTZ - 0.5f) * jitterRangeChunks);
    }

    private boolean streamInPattern(
            long patternKey, int homeChunkX, int homeChunkZ, double distanceChunks,
            int referenceChunkX, int referenceChunkZ) {

        if (freeSlots.isEmpty())
            return false;

        long chunkCoordinate = Coordinate2Long.pack(homeChunkX, homeChunkZ);
        long referenceCoordinate = Coordinate2Long.pack(referenceChunkX, referenceChunkZ);
        weatherManager.resolveWeatherBandTowardHorizon(bandScratch, chunkCoordinate, referenceCoordinate);

        WeatherHandle weatherHandle = bandScratch.getPrimary();
        float spread = bandScratch.getIntensityFor(weatherHandle);
        float intensity = spread * weatherHandle.getCloudCoverage();

        int slot = freeSlots.removeInt(freeSlots.size() - 1);
        WeatherPatternInstance pattern = patternPool[slot];

        pattern.constructor(patternKey, homeChunkX, homeChunkZ, weatherHandle, DEFAULT_DRIFT_SPEED_SCALE, intensity,
                spread);
        pattern.setNextReevaluationTime(elapsedSimTime + reevaluationIntervalFor(patternKey));
        pattern.setDistanceFromReferenceChunks((float) distanceChunks);
        pattern.updateBounds();

        activePatterns.put(patternKey, pattern);
        slotActive[slot] = true;
        streamedInThisFrame.add(pattern);

        return true;
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

        for (int i = 0; i < patternPool.length; i++) {

            if (!slotActive[i])
                continue;

            WeatherPatternInstance pattern = patternPool[i];
            pattern.advanceDrift(baseDeltaChunkX * pattern.getDriftSpeedScale(), 0.0);
        }
    }

    // Nearest Reference \\

    private long resolveNearestReferenceCoordinate(int chunkX, int chunkZ, ObjectArrayList<GridInstance> grids) {

        Object[] elements = grids.elements();
        int count = grids.size();

        if (count == 0)
            return Coordinate2Long.pack(chunkX, chunkZ);

        WorldHandle activeWorld = worldManager.getActiveWorld();
        int worldWidthChunks = activeWorld.getWorldScale().x / EngineSetting.CHUNK_SIZE;
        int worldHeightChunks = activeWorld.getWorldScale().y / EngineSetting.CHUNK_SIZE;

        long nearest = ((GridInstance) elements[0]).getActiveChunkCoordinate();
        double bestDistSq = Double.MAX_VALUE;

        for (int i = 0; i < count; i++) {

            long candidate = ((GridInstance) elements[i]).getActiveChunkCoordinate();
            int refX = Coordinate2Long.unpackX(candidate);
            int refZ = Coordinate2Long.unpackY(candidate);

            double dx = WorldWrapUtility.wrappedDelta(chunkX, refX, worldWidthChunks);
            double dz = WorldWrapUtility.wrappedDelta(chunkZ, refZ, worldHeightChunks);
            double distSq = dx * dx + dz * dz;

            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                nearest = candidate;
            }
        }

        return nearest;
    }

    private void advanceWeatherReevaluation(ObjectArrayList<GridInstance> grids) {

        float deltaTime = internal.getDeltaTime();
        elapsedSimTime += deltaTime;

        for (int i = 0; i < patternPool.length; i++) {

            if (!slotActive[i])
                continue;

            WeatherPatternInstance pattern = patternPool[i];

            pattern.advanceWeatherTransition(deltaTime);

            if (pattern.isRetiring())
                continue;

            if (elapsedSimTime < pattern.getNextReevaluationTime())
                continue;

            long homeCoordinate = Coordinate2Long.pack(pattern.getHomeChunkX(), pattern.getHomeChunkZ());
            long referenceCoordinate = resolveNearestReferenceCoordinate(
                    pattern.getHomeChunkX(), pattern.getHomeChunkZ(), grids);

            weatherManager.resolveWeatherBandTowardHorizonBiased(
                    bandScratch, homeCoordinate, referenceCoordinate, pattern.getWeatherHandle());

            WeatherHandle resolved = bandScratch.getPrimary();

            if (resolved != pattern.getWeatherHandle())
                tryRefreshWeather(pattern, resolved);

            pattern.setNextReevaluationTime(elapsedSimTime + reevaluationIntervalFor(pattern.getPatternKey()));
        }
    }

    private void tryRefreshWeather(WeatherPatternInstance pattern, WeatherHandle resolved) {
        pattern.beginWeatherTransition(resolved);
        refreshedThisFrame.add(pattern);
    }

    // Local Weather \\

    private void advanceLocalWeather(ObjectArrayList<GridInstance> grids) {

        float deltaTime = internal.getDeltaTime();
        temperatureSystem.advanceClock();

        Object[] elements = grids.elements();
        int count = grids.size();

        for (int i = 0; i < count; i++) {

            GridInstance grid = (GridInstance) elements[i];
            long referenceCoordinate = grid.getActiveChunkCoordinate();
            WeatherPatternInstance pattern = grid.getLocalWeatherPattern();

            if (!pattern.isConfigured()) {

                weatherManager.resolveWeatherBandTowardHorizon(bandScratch, referenceCoordinate, referenceCoordinate);
                WeatherHandle initial = bandScratch.getPrimary();
                float spread = bandScratch.getIntensityFor(initial);

                long localPatternKey = LOCAL_PATTERN_KEY_SEED
                        ^ (((long) System.identityHashCode(grid)) * 0x9E3779B97F4A7C15L);

                pattern.constructor(
                        localPatternKey,
                        Coordinate2Long.unpackX(referenceCoordinate),
                        Coordinate2Long.unpackY(referenceCoordinate),
                        initial,
                        DEFAULT_DRIFT_SPEED_SCALE,
                        spread * initial.getCloudCoverage(),
                        spread);

                pattern.setNextReevaluationTime(elapsedSimTime + reevaluationIntervalFor(localPatternKey));

            } else {

                pattern.advanceWeatherTransition(deltaTime);

                if (elapsedSimTime >= pattern.getNextReevaluationTime()) {

                    weatherManager.resolveWeatherBandTowardHorizonBiased(
                            bandScratch, referenceCoordinate, referenceCoordinate, pattern.getWeatherHandle());

                    WeatherHandle resolved = bandScratch.getPrimary();

                    if (resolved != pattern.getWeatherHandle())
                        pattern.beginWeatherTransition(resolved);

                    pattern.setNextReevaluationTime(elapsedSimTime + reevaluationIntervalFor(pattern.getPatternKey()));
                }
            }

            double visualTimeOfDay = grid.getClockInstance().getVisualTimeOfDay();
            float temperature = temperatureSystem.computeTemperature(pattern, visualTimeOfDay);
            gridToTemperature.put(grid, temperature);
        }

        pruneStaleTemperatures(grids);
    }

    private void pruneStaleTemperatures(ObjectArrayList<GridInstance> grids) {

        if (gridToTemperature.isEmpty())
            return;

        var iterator = gridToTemperature.keySet().iterator();

        while (iterator.hasNext())
            if (!grids.contains(iterator.next()))
                iterator.remove();
    }

    private void advanceIntensity(ObjectArrayList<GridInstance> grids) {

        intensityUpdateAccumulator += internal.getDeltaTime();

        if (intensityUpdateAccumulator < EngineSetting.WEATHER_PATTERN_INTENSITY_UPDATE_INTERVAL_SECONDS)
            return;

        intensityUpdateAccumulator = 0f;

        for (int i = 0; i < patternPool.length; i++) {

            if (!slotActive[i])
                continue;

            WeatherPatternInstance pattern = patternPool[i];

            if (pattern.isRetiring())
                continue;

            long homeCoordinate = Coordinate2Long.pack(pattern.getHomeChunkX(), pattern.getHomeChunkZ());
            long referenceCoordinate = resolveNearestReferenceCoordinate(
                    pattern.getHomeChunkX(), pattern.getHomeChunkZ(), grids);

            weatherManager.resolveWeatherBandTowardHorizon(bandScratch, homeCoordinate, referenceCoordinate);

            float purity = bandScratch.getIntensityFor(pattern.getWeatherHandle());

            pattern.setTargetSpread(purity);
            pattern.setTargetIntensity(purity * pattern.getWeatherHandle().getCloudCoverage());
        }
    }

    private void advanceIntensitySmoothing() {

        float deltaTime = internal.getDeltaTime();
        float alpha = 1f - (float) Math.exp(-deltaTime / intensitySmoothingTimeSeconds);

        for (int i = 0; i < patternPool.length; i++) {

            if (!slotActive[i])
                continue;

            patternPool[i].advanceIntensitySmoothing(alpha);
            patternPool[i].advanceSpreadSmoothing(alpha);
        }
    }

    private float reevaluationIntervalFor(long patternKey) {

        float driftChunksPerSecond = Math.abs(weatherManager.getWorldDriftChunksPerSecondX());
        float wavelengthChunks = EngineSetting.WEATHER_NOISE_CELL_SIZE;

        float baseSeconds = driftChunksPerSecond > 0.0001f
                ? (wavelengthChunks * reevaluationNoiseFraction) / driftChunksPerSecond
                : reevaluationMaxSeconds;

        float clampedSeconds = Math.max(reevaluationMinSeconds, Math.min(reevaluationMaxSeconds, baseSeconds));

        float jitterT = hash01(patternKey ^ 0xD1B54A32D192ED03L);
        float jitterScale = lerp(reevaluationJitterMin, reevaluationJitterMax, jitterT);

        return clampedSeconds * jitterScale;
    }

    private void advanceFadesAndRetire(ObjectArrayList<GridInstance> grids) {

        float deltaTime = internal.getDeltaTime();
        LongArrayList toRemove = null;

        WorldHandle activeWorld = worldManager.getActiveWorld();
        int worldWidthChunks = activeWorld.getWorldScale().x / EngineSetting.CHUNK_SIZE;
        int worldHeightChunks = activeWorld.getWorldScale().y / EngineSetting.CHUNK_SIZE;

        Object[] gridElements = grids.elements();
        int gridCount = grids.size();

        for (int i = 0; i < patternPool.length; i++) {

            if (!slotActive[i])
                continue;

            WeatherPatternInstance pattern = patternPool[i];

            double minDistChunks = 0.0;

            if (gridCount > 0) {

                minDistChunks = Double.MAX_VALUE;

                for (int g = 0; g < gridCount; g++) {

                    long referenceCoordinate = ((GridInstance) gridElements[g]).getActiveChunkCoordinate();
                    int refChunkX = Coordinate2Long.unpackX(referenceCoordinate);
                    int refChunkZ = Coordinate2Long.unpackY(referenceCoordinate);

                    double dx = WorldWrapUtility.wrappedDelta(pattern.getCurrentChunkX(), refChunkX, worldWidthChunks);
                    double dz = WorldWrapUtility.wrappedDelta(pattern.getCurrentChunkZ(), refChunkZ, worldHeightChunks);
                    double distChunks = Math.sqrt(dx * dx + dz * dz);

                    if (distChunks < minDistChunks)
                        minDistChunks = distChunks;
                }
            }

            pattern.setDistanceFromReferenceChunks((float) minDistChunks);
            pattern.updateBounds();

            if (minDistChunks > outerRangeChunks && !pattern.isRetiring())
                pattern.setRetiring(true);

            float alpha = pattern.getFadeAlpha();

            if (pattern.isRetiring()) {

                alpha = Math.max(0f, alpha - fadeOutRate * deltaTime);
                pattern.setFadeAlpha(alpha);

                if (alpha <= 0f) {
                    if (toRemove == null)
                        toRemove = new LongArrayList();
                    toRemove.add(pattern.getPatternKey());
                }

            } else if (alpha < 1f) {
                pattern.setFadeAlpha(Math.min(1f, alpha + fadeInRate * deltaTime));
            }
        }

        if (toRemove != null)
            for (int i = 0; i < toRemove.size(); i++)
                removePattern(toRemove.getLong(i));
    }

    private void removePattern(long patternKey) {

        WeatherPatternInstance pattern = activePatterns.remove(patternKey);

        if (pattern == null)
            return;

        slotActive[pattern.getSlot()] = false;
        pendingFreeSlots.add(pattern.getSlot());
        retiredThisFrame.add(pattern);
    }

    static float hash01(long seed) {

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

    public Long2ObjectOpenHashMap<WeatherPatternInstance> getActivePatterns() {
        return activePatterns;
    }

    public WeatherPatternInstance[] getPatternPool() {
        return patternPool;
    }

    public boolean isPatternActive(int slot) {
        return slotActive[slot];
    }

    public ObjectArrayList<WeatherPatternInstance> getPatternsStreamedInThisFrame() {
        return streamedInThisFrame;
    }

    public ObjectArrayList<WeatherPatternInstance> getPatternsRetiredThisFrame() {
        return retiredThisFrame;
    }

    public ObjectArrayList<WeatherPatternInstance> getPatternsRefreshedThisFrame() {
        return refreshedThisFrame;
    }

    public int getActivePatternCount() {
        return activePatterns.size();
    }

    public float getOuterRangeChunks() {
        return outerRangeChunks;
    }

    public float getNearRangeChunks() {
        return nearRangeChunks;
    }

    // Grid Factory \\

    public WeatherPatternInstance createLocalPatternInstance() {
        return create(WeatherPatternInstance.class);
    }

    // Local Weather Accessible \\

    private WeatherPatternInstance resolvePrimaryLocalPattern() {

        if (!worldStreamManager.hasGrids())
            return null;

        WeatherPatternInstance pattern = worldStreamManager.getGrids().get(0).getLocalWeatherPattern();
        return pattern.isConfigured() ? pattern : null;
    }

    public boolean hasLocalWeather() {
        return resolvePrimaryLocalPattern() != null;
    }

    public WeatherHandle getCurrentWeatherHandle() {
        WeatherPatternInstance pattern = resolvePrimaryLocalPattern();
        return pattern != null ? pattern.getWeatherHandle() : null;
    }

    public float getWindSpeedScale() {
        WeatherPatternInstance pattern = resolvePrimaryLocalPattern();
        return pattern != null
                ? pattern.getBlendedWindSpeedScale()
                : EngineSetting.DEFAULT_WEATHER_WIND_SPEED_SCALE;
    }

    public float getWindTurbulenceScale() {
        WeatherPatternInstance pattern = resolvePrimaryLocalPattern();
        return pattern != null
                ? pattern.getBlendedWindTurbulenceScale()
                : EngineSetting.DEFAULT_WEATHER_WIND_TURBULENCE_SCALE;
    }

    public float getHumidity() {
        WeatherPatternInstance pattern = resolvePrimaryLocalPattern();
        return pattern != null
                ? pattern.getBlendedHumidity()
                : EngineSetting.DEFAULT_WEATHER_HUMIDITY;
    }

    public float getVisibility() {
        WeatherPatternInstance pattern = resolvePrimaryLocalPattern();
        return pattern != null
                ? pattern.getBlendedVisibility()
                : EngineSetting.DEFAULT_WEATHER_VISIBILITY;
    }

    public float getFogDensityScale() {
        WeatherPatternInstance pattern = resolvePrimaryLocalPattern();
        return pattern != null
                ? pattern.getBlendedFogDensityScale()
                : EngineSetting.DEFAULT_WEATHER_FOG_DENSITY_SCALE;
    }

    public float getCurrentTemperature(GridInstance grid) {

        if (!weatherManager.hasActiveWeatherPool())
            return EngineSetting.DEFAULT_BASE_TEMPERATURE;

        Float temperature = gridToTemperature.get(grid);

        return temperature != null ? temperature : EngineSetting.DEFAULT_BASE_TEMPERATURE;
    }
}