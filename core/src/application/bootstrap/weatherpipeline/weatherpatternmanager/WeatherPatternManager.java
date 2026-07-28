package application.bootstrap.weatherpipeline.weatherpatternmanager;

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
     * The Determined Weather Pattern system. Streams jittered spatial cells
     * around the player and tracks which weather each resolves to for the
     * GPU weather map, drifting them with world rotation until they retire.
     * Also resolves one pattern pinned to the player's own reference
     * coordinate, never streamed, whose blended values drive temperature
     * and local wind. Reevaluation cadence for both derives from the
     * world's KPH-based drift speed rather than a fixed timer.
     */

    private static final float DEFAULT_DRIFT_SPEED_SCALE = 1.0f;

    private static final long LOCAL_PATTERN_KEY = Long.MIN_VALUE;
    private static final int LOCAL_PATTERN_SLOT = -1;

    private WeatherManager weatherManager;
    private WorldManager worldManager;
    private TemperatureBranch temperatureBranch;
    private WeatherMapBufferSystem weatherMapBufferSystem;

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

    private Long2ObjectOpenHashMap<WeatherPatternStruct> activePatterns;
    private WeatherPatternStruct localPattern;

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
        this.reevaluationNoiseFraction = EngineSetting.WEATHER_PATTERN_REEVALUATION_NOISE_FRACTION;
        this.reevaluationJitterMin = EngineSetting.WEATHER_PATTERN_REEVALUATION_JITTER_MIN;
        this.reevaluationJitterMax = EngineSetting.WEATHER_PATTERN_REEVALUATION_JITTER_MAX;
        this.reevaluationMinSeconds = EngineSetting.WEATHER_PATTERN_REEVALUATION_MIN_SECONDS;
        this.reevaluationMaxSeconds = EngineSetting.WEATHER_PATTERN_REEVALUATION_MAX_SECONDS;
        this.intensitySmoothingTimeSeconds = EngineSetting.WEATHER_PATTERN_INTENSITY_SMOOTHING_TIME_SECONDS;
        this.fadeInRate = EngineSetting.WEATHER_PATTERN_FADE_IN_RATE;
        this.fadeOutRate = EngineSetting.WEATHER_PATTERN_FADE_OUT_RATE;

        this.activePatterns = new Long2ObjectOpenHashMap<>();

        this.freeSlots = new IntArrayList(maxActivePatternCount);
        for (int i = 0; i < maxActivePatternCount; i++)
            freeSlots.add(i);

        this.scanCursor = 0;

        this.elapsedSimTime = 0.0;
        this.intensityUpdateAccumulator = 0f;

        this.streamedInThisFrame = new ObjectArrayList<>();
        this.retiredThisFrame = new ObjectArrayList<>();
        this.refreshedThisFrame = new ObjectArrayList<>();

        this.temperatureBranch = create(TemperatureBranch.class);
        this.weatherMapBufferSystem = create(WeatherMapBufferSystem.class);
    }

    @Override
    protected void get() {
        this.weatherManager = get(WeatherManager.class);
        this.worldManager = get(WorldManager.class);
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

        if (!weatherManager.hasActiveWeatherPool()) {
            localPattern = null;
            return;
        }

        long referenceCoordinate = weatherManager.getReferenceCoordinate();
        int playerChunkX = Coordinate2Long.unpackX(referenceCoordinate);
        int playerChunkZ = Coordinate2Long.unpackY(referenceCoordinate);

        advanceWorldDrift();
        advanceWeatherReevaluation();
        advanceLocalWeather(referenceCoordinate);
        advanceIntensity();
        advanceIntensitySmoothing();
        advanceFadesAndRetire(playerChunkX, playerChunkZ);
        streamInBudgeted(playerChunkX, playerChunkZ);

        temperatureBranch.updateTemperature(localPattern);
    }

    // Candidate cells sorted upwind-first, then nearest-first.
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

            if (trueDistanceChunks > outerRangeChunks)
                continue;

            long wrappedHome = wrapChunkCoordinate(homeChunkX, homeChunkZ);
            int wrappedHomeChunkX = Coordinate2Long.unpackX(wrappedHome);
            int wrappedHomeChunkZ = Coordinate2Long.unpackY(wrappedHome);

            if (streamInPattern(patternKey, wrappedHomeChunkX, wrappedHomeChunkZ, trueDistanceChunks))
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

    private boolean streamInPattern(long patternKey, int homeChunkX, int homeChunkZ, double distanceChunks) {

        if (freeSlots.isEmpty())
            return false;

        long chunkCoordinate = Coordinate2Long.pack(homeChunkX, homeChunkZ);
        weatherManager.resolveWeatherBandTowardHorizon(bandScratch, chunkCoordinate);

        WeatherHandle weatherHandle = bandScratch.getPrimary();
        float spread = bandScratch.getIntensityFor(weatherHandle);
        float intensity = spread * weatherHandle.getCloudCoverage();

        int slot = freeSlots.removeInt(freeSlots.size() - 1);

        WeatherPatternStruct pattern = new WeatherPatternStruct(
                patternKey, homeChunkX, homeChunkZ, weatherHandle,
                DEFAULT_DRIFT_SPEED_SCALE, intensity, spread, slot);

        pattern.setNextReevaluationTime(elapsedSimTime + reevaluationIntervalFor(patternKey));
        pattern.setDistanceFromReferenceChunks((float) distanceChunks);
        pattern.updateBounds();

        activePatterns.put(patternKey, pattern);
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
        pattern.beginWeatherTransition(resolved);
        refreshedThisFrame.add(pattern);
    }

    // Local Weather \\

    private void advanceLocalWeather(long referenceCoordinate) {

        float deltaTime = internal.getDeltaTime();

        if (localPattern == null) {

            weatherManager.resolveWeatherBandTowardHorizon(bandScratch, referenceCoordinate);
            WeatherHandle initial = bandScratch.getPrimary();
            float spread = bandScratch.getIntensityFor(initial);

            this.localPattern = new WeatherPatternStruct(
                    LOCAL_PATTERN_KEY,
                    Coordinate2Long.unpackX(referenceCoordinate),
                    Coordinate2Long.unpackY(referenceCoordinate),
                    initial,
                    DEFAULT_DRIFT_SPEED_SCALE,
                    spread * initial.getCloudCoverage(),
                    spread,
                    LOCAL_PATTERN_SLOT);

            localPattern.setNextReevaluationTime(elapsedSimTime + reevaluationIntervalFor(LOCAL_PATTERN_KEY));
            return;
        }

        localPattern.advanceWeatherTransition(deltaTime);

        if (elapsedSimTime < localPattern.getNextReevaluationTime())
            return;

        weatherManager.resolveWeatherBandTowardHorizonBiased(
                bandScratch, referenceCoordinate, localPattern.getWeatherHandle());

        WeatherHandle resolved = bandScratch.getPrimary();

        if (resolved != localPattern.getWeatherHandle())
            localPattern.beginWeatherTransition(resolved);

        localPattern.setNextReevaluationTime(elapsedSimTime + reevaluationIntervalFor(LOCAL_PATTERN_KEY));
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
        float alpha = 1f - (float) Math.exp(-deltaTime / intensitySmoothingTimeSeconds);

        for (WeatherPatternStruct pattern : activePatterns.values()) {
            pattern.advanceIntensitySmoothing(alpha);
            pattern.advanceSpreadSmoothing(alpha);
        }
    }

    // Base cadence: time for the world to drift one
    // WEATHER_PATTERN_REEVALUATION_NOISE_FRACTION of a full noise
    // wavelength at its current KPH-derived speed, clamped, then jittered
    // per pattern so the active set never reevaluates in lockstep.
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

            pattern.setDistanceFromReferenceChunks((float) distChunks);
            pattern.updateBounds();

            if (distChunks > outerRangeChunks && !pattern.isRetiring())
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

        WeatherPatternStruct pattern = activePatterns.remove(patternKey);

        if (pattern == null)
            return;

        freeSlots.add(pattern.getSlot());
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

    public float getOuterRangeChunks() {
        return outerRangeChunks;
    }

    public float getNearRangeChunks() {
        return nearRangeChunks;
    }

    public int getNearRangeWeatherMapEntryCount() {
        return weatherMapBufferSystem.getNearRangeEntryCount();
    }

    // Local Weather Accessible \\

    public boolean hasLocalWeather() {
        return localPattern != null;
    }

    public WeatherHandle getCurrentWeatherHandle() {
        return localPattern != null ? localPattern.getWeatherHandle() : null;
    }

    public float getWindSpeedScale() {
        return localPattern != null
                ? localPattern.getBlendedWindSpeedScale()
                : EngineSetting.DEFAULT_WEATHER_WIND_SPEED_SCALE;
    }

    public float getWindTurbulenceScale() {
        return localPattern != null
                ? localPattern.getBlendedWindTurbulenceScale()
                : EngineSetting.DEFAULT_WEATHER_WIND_TURBULENCE_SCALE;
    }

    public float getHumidity() {
        return localPattern != null
                ? localPattern.getBlendedHumidity()
                : EngineSetting.DEFAULT_WEATHER_HUMIDITY;
    }

    public float getVisibility() {
        return localPattern != null
                ? localPattern.getBlendedVisibility()
                : EngineSetting.DEFAULT_WEATHER_VISIBILITY;
    }

    public float getFogDensityScale() {
        return localPattern != null
                ? localPattern.getBlendedFogDensityScale()
                : EngineSetting.DEFAULT_WEATHER_FOG_DENSITY_SCALE;
    }

    public float getCurrentTemperature() {
        if (!weatherManager.hasActiveWeatherPool())
            return EngineSetting.DEFAULT_BASE_TEMPERATURE;
        return temperatureBranch.getCurrentTemperature();
    }
}