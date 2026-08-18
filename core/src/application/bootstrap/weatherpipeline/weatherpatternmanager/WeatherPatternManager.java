package application.bootstrap.weatherpipeline.weatherpatternmanager;

import application.bootstrap.weatherpipeline.temperature.TemperatureInstance;
import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import application.bootstrap.weatherpipeline.weathermanager.WeatherManager;
import application.bootstrap.weatherpipeline.weather.WeatherInstance;
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
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class WeatherPatternManager extends ManagerPackage {

    /*
     * Streams pool-recycled spatial weather cells around every active grid,
     * plus one always-present local instance per grid that spawns at that
     * grid's own reference chunk and then drifts with the same world
     * rotation every other pattern does. Position, fades, and weather-type
     * crossfades all advance every frame from the same KPH-derived drift
     * speed; range membership (streaming in and retiring) is only
     * reassessed on the shared tick so a pattern sitting near the range
     * boundary can't flicker in and out — it either stays fully present or
     * commits to a full fade-out, and can resume normal presence if it's
     * back in range by the next tick.
     *
     * Every tunable and every seed/salt used below comes from EngineSetting
     * — WEATHER_PATTERN_DEFAULT_DRIFT_SPEED_SCALE, WEATHER_PATTERN_LOCAL_KEY_SEED,
     * WEATHER_HASH_SALT_PRIMARY/SECONDARY, HASH_FINALIZER_MULTIPLIER_1/2 —
     * nothing here is a locally authored duplicate.
     */

    private WeatherManager weatherManager;
    private WorldManager worldManager;
    private WorldStreamManager worldStreamManager;
    private TemperatureSystem temperatureSystem;

    private int patternCellSizeChunks;
    private float rangeChunks;
    private int maxActivePatternCount;
    private float tickIntervalSeconds;
    private double nextTickTime;
    private float fadeInRate;
    private float fadeOutRate;

    private Long2ObjectOpenHashMap<WeatherInstance> activePatterns;

    private WeatherInstance[] patternPool;
    private boolean[] slotActive;
    private IntArrayList freeSlots;
    private IntArrayList pendingFreeSlots;

    private ObjectArrayList<int[]> candidateOffsets;

    private double elapsedSimTime;

    private final int[] homeJitterScratch = new int[2];

    private ObjectArrayList<WeatherInstance> streamedInThisFrame;
    private ObjectArrayList<WeatherInstance> retiredThisFrame;
    private ObjectArrayList<WeatherInstance> refreshedThisFrame;

    @Override
    protected void create() {

        this.patternCellSizeChunks = EngineSetting.WEATHER_PATTERN_CELL_SIZE_CHUNKS;
        this.maxActivePatternCount = EngineSetting.WEATHER_PATTERN_MAX_ACTIVE_COUNT;
        this.fadeInRate = EngineSetting.WEATHER_PATTERN_FADE_IN_RATE;
        this.fadeOutRate = EngineSetting.WEATHER_PATTERN_FADE_OUT_RATE;

        this.activePatterns = new Long2ObjectOpenHashMap<>();

        this.freeSlots = new IntArrayList(maxActivePatternCount);
        this.pendingFreeSlots = new IntArrayList(maxActivePatternCount);
        this.patternPool = new WeatherInstance[maxActivePatternCount];
        this.slotActive = new boolean[maxActivePatternCount];

        for (int i = 0; i < maxActivePatternCount; i++) {
            freeSlots.add(i);
            WeatherInstance pattern = create(WeatherInstance.class);
            pattern.assignSlot(i);
            patternPool[i] = pattern;
        }

        this.elapsedSimTime = 0.0;

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
        this.rangeChunks = weatherManager.getEffectiveRangeChunks();
        this.candidateOffsets = buildCandidateOffsets();
        this.tickIntervalSeconds = computeTickIntervalSeconds();
        this.nextTickTime = 0.0;
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

        if (!weatherManager.hasActiveWeatherPool())
            return;

        ObjectArrayList<GridInstance> grids = worldStreamManager.getGrids();

        elapsedSimTime += internal.getDeltaTime();
        boolean tickFired = elapsedSimTime >= nextTickTime;

        advanceWorldDrift();
        advancePoolPatterns(tickFired, grids);
        advanceLocalWeather(grids, tickFired);
        updatePatternSpatialState(grids);

        if (tickFired) {
            reassessRangeMembership();
            streamInAll(grids);
            nextTickTime = elapsedSimTime + tickIntervalSeconds;
        }

        advanceFades();
    }

    // Candidate Offsets \\

    private ObjectArrayList<int[]> buildCandidateOffsets() {

        float jitterRangeChunks = patternCellSizeChunks * EngineSetting.WEATHER_PATTERN_HOME_JITTER_RATIO;
        float maxJitterMagnitudeChunks = (jitterRangeChunks * 0.5f) * (float) Math.sqrt(2.0);
        float candidateRadiusChunks = rangeChunks + maxJitterMagnitudeChunks;
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

        offsets.sort((a, b) -> Integer.compare(a[2], b[2]));

        return offsets;
    }

    // Streaming — tick-only \\

    private void streamInAll(ObjectArrayList<GridInstance> grids) {

        if (activePatterns.size() >= maxActivePatternCount)
            return;

        Object[] elements = grids.elements();
        int gridCount = grids.size();

        for (int g = 0; g < gridCount && activePatterns.size() < maxActivePatternCount; g++) {

            long referenceCoordinate = ((GridInstance) elements[g]).getActiveChunkCoordinate();
            int playerChunkX = Coordinate2Long.unpackX(referenceCoordinate);
            int playerChunkZ = Coordinate2Long.unpackY(referenceCoordinate);

            streamInForReference(playerChunkX, playerChunkZ);
        }
    }

    private void streamInForReference(int playerChunkX, int playerChunkZ) {

        int playerCellX = Math.floorDiv(playerChunkX, patternCellSizeChunks);
        int playerCellZ = Math.floorDiv(playerChunkZ, patternCellSizeChunks);

        WorldHandle activeWorld = worldManager.getActiveWorld();
        int worldWidthChunks = activeWorld.getWorldScale().x / EngineSetting.CHUNK_SIZE;
        int worldHeightChunks = activeWorld.getWorldScale().y / EngineSetting.CHUNK_SIZE;

        int candidateCount = candidateOffsets.size();

        for (int c = 0; c < candidateCount && activePatterns.size() < maxActivePatternCount; c++) {

            int[] offset = candidateOffsets.get(c);
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

            if (trueDistanceChunks > rangeChunks)
                continue;

            long wrappedHome = wrapChunkCoordinate(homeChunkX, homeChunkZ);
            int wrappedHomeChunkX = Coordinate2Long.unpackX(wrappedHome);
            int wrappedHomeChunkZ = Coordinate2Long.unpackY(wrappedHome);

            streamInPattern(patternKey, wrappedHomeChunkX, wrappedHomeChunkZ, trueDistanceChunks,
                    playerChunkX, playerChunkZ);
        }
    }

    private void computeHomeJitter(long patternKey) {

        long jitterSeed = patternKey ^ EngineSetting.WEATHER_HASH_SALT_PRIMARY;

        float jitterTX = hash01(jitterSeed);
        float jitterTZ = hash01(jitterSeed ^ EngineSetting.WEATHER_HASH_SALT_SECONDARY);

        float jitterRangeChunks = patternCellSizeChunks * EngineSetting.WEATHER_PATTERN_HOME_JITTER_RATIO;

        homeJitterScratch[0] = Math.round((jitterTX - 0.5f) * jitterRangeChunks);
        homeJitterScratch[1] = Math.round((jitterTZ - 0.5f) * jitterRangeChunks);
    }

    private void streamInPattern(
            long patternKey, int homeChunkX, int homeChunkZ, double distanceChunks,
            int referenceChunkX, int referenceChunkZ) {

        if (freeSlots.isEmpty())
            return;

        long chunkCoordinate = Coordinate2Long.pack(homeChunkX, homeChunkZ);
        long referenceCoordinate = Coordinate2Long.pack(referenceChunkX, referenceChunkZ);

        WeatherHandle weatherHandle = weatherManager.resolveWeatherTowardHorizon(chunkCoordinate, referenceCoordinate);

        int slot = freeSlots.removeInt(freeSlots.size() - 1);
        WeatherInstance pattern = patternPool[slot];

        pattern.constructor(patternKey, homeChunkX, homeChunkZ, weatherHandle,
                EngineSetting.WEATHER_PATTERN_DEFAULT_DRIFT_SPEED_SCALE);
        assignVelocity(pattern);
        pattern.setDistanceFromReferenceChunks((float) distanceChunks);
        pattern.updateBounds();

        activePatterns.put(patternKey, pattern);
        slotActive[slot] = true;
        streamedInThisFrame.add(pattern);
    }

    private long wrapChunkCoordinate(int chunkX, int chunkZ) {

        WorldHandle activeWorld = worldManager.getActiveWorld();
        int worldWidthChunks = activeWorld.getWorldScale().x / EngineSetting.CHUNK_SIZE;
        int worldHeightChunks = activeWorld.getWorldScale().y / EngineSetting.CHUNK_SIZE;

        int wrappedX = Math.floorMod(chunkX, worldWidthChunks);
        int wrappedZ = Math.floorMod(chunkZ, worldHeightChunks);

        return Coordinate2Long.pack(wrappedX, wrappedZ);
    }

    // Velocity \\

    private void assignVelocity(WeatherInstance pattern) {
        double baseVelocityXChunksPerSecond = -weatherManager.getWorldDriftChunksPerSecondX();
        pattern.setVelocity(baseVelocityXChunksPerSecond * pattern.getDriftSpeedScale(), 0.0);
    }

    private void advanceWorldDrift() {

        double deltaTime = internal.getDeltaTime();

        for (int i = 0; i < patternPool.length; i++) {

            if (!slotActive[i])
                continue;

            patternPool[i].advancePosition(deltaTime);
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

    // Global Tick — weather-type reassessment \\

    private void advancePoolPatterns(boolean tickFired, ObjectArrayList<GridInstance> grids) {

        float deltaTime = internal.getDeltaTime();

        for (int i = 0; i < patternPool.length; i++) {

            if (!slotActive[i])
                continue;

            WeatherInstance pattern = patternPool[i];

            pattern.advanceWeatherTransition(deltaTime);

            if (!tickFired || pattern.isRetiring())
                continue;

            int currentChunkX = (int) Math.round(pattern.getCurrentChunkX());
            int currentChunkZ = (int) Math.round(pattern.getCurrentChunkZ());

            long currentCoordinate = Coordinate2Long.pack(currentChunkX, currentChunkZ);
            long referenceCoordinate = resolveNearestReferenceCoordinate(currentChunkX, currentChunkZ, grids);

            WeatherHandle resolved = weatherManager.resolveWeatherTowardHorizonBiased(
                    currentCoordinate, referenceCoordinate, pattern.getWeatherHandle());

            if (resolved != pattern.getWeatherHandle())
                tryRefreshWeather(pattern, resolved);
        }
    }

    private void tryRefreshWeather(WeatherInstance pattern, WeatherHandle resolved) {
        pattern.beginWeatherTransition(resolved);
        refreshedThisFrame.add(pattern);
    }

    private float computeTickIntervalSeconds() {

        float driftChunksPerSecond = Math.abs(weatherManager.getWorldDriftChunksPerSecondX());
        float wavelengthChunks = EngineSetting.WEATHER_NOISE_CELL_SIZE;

        float baseSeconds = driftChunksPerSecond > 0.0001f
                ? (wavelengthChunks * EngineSetting.WEATHER_TICK_NOISE_FRACTION) / driftChunksPerSecond
                : EngineSetting.WEATHER_TICK_MAX_SECONDS;

        return Math.max(EngineSetting.WEATHER_TICK_MIN_SECONDS,
                Math.min(EngineSetting.WEATHER_TICK_MAX_SECONDS, baseSeconds));
    }

    // Local Weather \\

    /*
     * The per-grid local instance previously never moved — zero velocity,
     * position fixed at construction, always drawn dead-centered by
     * WeatherMapBufferSystem — so its clouds were frozen every frame
     * regardless of anything else in the pipeline. It now gets the exact
     * same KPH-derived velocity every pooled pattern gets and advances its
     * position every frame identically, so WeatherMapBufferSystem can
     * place it with the same reference-relative math as pooled patterns
     * instead of a hardcoded (0,0). Its weather TYPE still re-resolves
     * against the grid's own current reference coordinate every tick, so
     * the atmosphere values (wind/temperature) it feeds always reflect
     * where the player actually is, independent of its own drifted
     * render position.
     */
    private void advanceLocalWeather(ObjectArrayList<GridInstance> grids, boolean tickFired) {

        float deltaTime = internal.getDeltaTime();
        temperatureSystem.advanceClock();

        Object[] elements = grids.elements();
        int count = grids.size();

        for (int i = 0; i < count; i++) {

            GridInstance grid = (GridInstance) elements[i];
            long referenceCoordinate = grid.getActiveChunkCoordinate();
            WeatherInstance pattern = grid.getWeatherInstance();

            if (!pattern.isConfigured()) {

                WeatherHandle initial = weatherManager.resolveWeatherTowardHorizon(
                        referenceCoordinate, referenceCoordinate);

                long localPatternKey = EngineSetting.WEATHER_PATTERN_LOCAL_KEY_SEED
                        ^ (((long) System.identityHashCode(grid)) * EngineSetting.WEATHER_HASH_SALT_SECONDARY);

                pattern.constructor(
                        localPatternKey,
                        Coordinate2Long.unpackX(referenceCoordinate),
                        Coordinate2Long.unpackY(referenceCoordinate),
                        initial,
                        EngineSetting.WEATHER_PATTERN_DEFAULT_DRIFT_SPEED_SCALE);

                pattern.setFadeAlpha(1f);
                assignVelocity(pattern);

            } else {

                pattern.advancePosition(deltaTime);
                pattern.advanceWeatherTransition(deltaTime);

                if (tickFired) {

                    WeatherHandle resolved = weatherManager.resolveWeatherTowardHorizonBiased(
                            referenceCoordinate, referenceCoordinate, pattern.getWeatherHandle());

                    if (resolved != pattern.getWeatherHandle())
                        pattern.beginWeatherTransition(resolved);
                }
            }

            double visualTimeOfDay = grid.getClockInstance().getVisualTimeOfDay();
            float temperature = temperatureSystem.computeTemperature(pattern, visualTimeOfDay);
            grid.getTemperatureInstance().setTemperature(temperature);
        }
    }

    // Spatial State — continuous \\

    private void updatePatternSpatialState(ObjectArrayList<GridInstance> grids) {

        WorldHandle activeWorld = worldManager.getActiveWorld();
        int worldWidthChunks = activeWorld.getWorldScale().x / EngineSetting.CHUNK_SIZE;
        int worldHeightChunks = activeWorld.getWorldScale().y / EngineSetting.CHUNK_SIZE;

        Object[] gridElements = grids.elements();
        int gridCount = grids.size();

        for (int i = 0; i < patternPool.length; i++) {

            if (!slotActive[i])
                continue;

            WeatherInstance pattern = patternPool[i];
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
        }
    }

    // Range Membership — tick-only \\

    /*
     * A pattern only starts retiring once its own visible EDGE — home
     * distance minus its own footprint radius — has left range, exactly
     * matching the edge-distance cull WeatherMapBufferSystem already uses
     * to decide whether to draw it. Comparing raw home distance alone let
     * a wide pattern start fading while its edge was still fully rendered
     * and visible near the player — weather visibly popping/disappearing
     * instead of drifting off into the distance the way it should.
     */
    private void reassessRangeMembership() {

        for (int i = 0; i < patternPool.length; i++) {

            if (!slotActive[i])
                continue;

            WeatherInstance pattern = patternPool[i];
            float edgeDistanceChunks = pattern.getDistanceFromReferenceChunks() - pattern.getFootprintRadiusChunks();
            boolean inRange = edgeDistanceChunks <= rangeChunks;

            pattern.setRetiring(!inRange);
        }
    }

    // Fades — continuous \\

    private void advanceFades() {

        float deltaTime = internal.getDeltaTime();
        LongArrayList toRemove = null;

        for (int i = 0; i < patternPool.length; i++) {

            if (!slotActive[i])
                continue;

            WeatherInstance pattern = patternPool[i];
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

        WeatherInstance pattern = activePatterns.remove(patternKey);

        if (pattern == null)
            return;

        slotActive[pattern.getSlot()] = false;
        pendingFreeSlots.add(pattern.getSlot());
        retiredThisFrame.add(pattern);
    }

    static float hash01(long seed) {

        long h = seed;
        h ^= (h >>> 33);
        h *= EngineSetting.HASH_FINALIZER_MULTIPLIER_1;
        h ^= (h >>> 33);
        h *= EngineSetting.HASH_FINALIZER_MULTIPLIER_2;
        h ^= (h >>> 33);

        return (float) ((h >>> 11) / (double) (1L << 53));
    }

    public Long2ObjectOpenHashMap<WeatherInstance> getActivePatterns() {
        return activePatterns;
    }

    public WeatherInstance[] getPatternPool() {
        return patternPool;
    }

    public boolean isPatternActive(int slot) {
        return slotActive[slot];
    }

    public ObjectArrayList<WeatherInstance> getPatternsStreamedInThisFrame() {
        return streamedInThisFrame;
    }

    public ObjectArrayList<WeatherInstance> getPatternsRetiredThisFrame() {
        return retiredThisFrame;
    }

    public ObjectArrayList<WeatherInstance> getPatternsRefreshedThisFrame() {
        return refreshedThisFrame;
    }

    public int getActivePatternCount() {
        return activePatterns.size();
    }

    public float getRangeChunks() {
        return rangeChunks;
    }

    // Grid Factory \\

    public WeatherInstance createLocalWeatherInstance() {
        return create(WeatherInstance.class);
    }

    public TemperatureInstance createTemperatureInstance() {
        TemperatureInstance instance = create(TemperatureInstance.class);
        instance.constructor();
        return instance;
    }
}