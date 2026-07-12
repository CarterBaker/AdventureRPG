package application.bootstrap.weatherpipeline.overheadmanager;

import application.bootstrap.weatherpipeline.cloud.CloudHandle;
import application.bootstrap.weatherpipeline.weather.CloudChanceStruct;
import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import application.bootstrap.weatherpipeline.weathermanager.WeatherBandStruct;
import application.bootstrap.weatherpipeline.weathermanager.WeatherManager;
import application.bootstrap.weatherpipeline.windmanager.WindManager;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
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
     * Streams a capped set of persistent-identity "weather patterns" — up
     * to EngineSetting.WEATHER_PATTERN_MAX_ACTIVE_COUNT of them — around
     * the weather system's current reference coordinate (see
     * WeatherManager.updateReferenceCoordinate()). Each pattern is one
     * whole, large-scale weather system (a storm, a fair-weather puff
     * field, and so on), not a single small cloud.
     *
     * A pattern owns one fixed WeatherHandle choice for its entire
     * lifetime, resolved once at stream-in via
     * WeatherManager.resolveWeatherBandTowardHorizon() — the SAME
     * blended-toward-the-sky-dome resolution RegionSampleBranch's own doc
     * comment describes as necessary for a real, positioned cloud object:
     * a pattern streamed in right next to the player shows the weather
     * that is ACTUALLY there, while a pattern streamed in near the outer
     * edge of the streaming radius instead agrees with whatever the sky
     * dome is already showing along that exact bearing.
     * advanceWeatherReevaluation() and advanceIntensity() both re-resolve
     * through this same horizon-blended path for as long as a pattern is
     * alive, so a pattern's identity and intensity never disagree with
     * the sky dome preview it grew out of, at any point in its lifetime —
     * not just at the moment it streamed in.
     *
     * Stage 1 retrofit — from dense grid cells to capped weather patterns
     * -------------------------------------------------------------------
     * The previous design streamed a uniform grid of small (four-chunk)
     * cells, each independently resolving its own tiny patch of weather,
     * with no cap on how many could be active at once inside the
     * streaming radius — a large configured render distance could stream
     * in several hundred concurrent cells, each drawing exactly one cloud
     * archetype as one plain cube. That satisfied neither "weather should
     * be rather large" nor "capped to something like 64 total". This
     * class now manages WeatherPatternStruct instances instead: far fewer
     * of them, each spanning a much larger patternCellSizeChunks
     * footprint (EngineSetting.WEATHER_PATTERN_CELL_SIZE_CHUNKS), and
     * hard-capped at maxActivePatternCount regardless of render distance
     * — streamInBudgeted() simply stops admitting new patterns once the
     * cap is hit, so an oversized render distance gracefully thins out
     * toward its edge instead of ever exceeding the cap.
     *
     * Each pattern also no longer renders as a single cube. It builds a
     * small, fixed set of lobes once at stream-in — see
     * WeatherPatternLobeStruct's own doc comment — offset around the
     * pattern's own home center to approximate an irregular, organic
     * weather shape instead of one uniform box, with each lobe free to
     * draw a DIFFERENT cloud archetype pulled from the same weather's own
     * chance-weighted cloud pool. OverheadCellStruct — the type
     * CloudRenderSystem already consumes, one instance per rendered cloud
     * volume — is now a thin, live read-through view of exactly one lobe
     * of exactly one pattern, so CloudRenderSystem itself needed zero
     * changes for this retrofit; only how those cells come to exist did.
     *
     * Home Jitter
     * -----------
     * A pattern's home center is no longer pinned to the exact center of
     * its own patternCellSizeChunks cell — computeHomeJitter() displaces
     * it by up to EngineSetting.WEATHER_PATTERN_HOME_JITTER_RATIO of the
     * cell's own size, derived purely from the pattern's own patternKey
     * (stable — the same cell always jitters the same way; never
     * re-rolled). Without this, the capped set of patterns visibly lined
     * up on a perfect grid regardless of how organic each individual
     * pattern's own lobe shape was — exactly the "defined patch or grid"
     * look the underlying weather noise field itself already avoids (see
     * GlobalNoiseBranch/RegionSampleBranch). Jitter is applied once, at
     * stream-in, before candidate distance sorting has any further say —
     * it does not change which cell gets picked, only where inside that
     * cell the resulting pattern actually sits.
     *
     * Streaming is still split into the same four passes as before, just
     * operating on patterns rather than cells:
     *
     * - Every frame, ALL active patterns get a cheap pass: advance their
     * wind drift (shared by every one of their lobes — a pattern moves
     * through the sky as one cohesive system, never as several
     * independently-timed cloud archetypes drifting apart from each
     * other — see WeatherPatternStruct.getDriftSpeedScale()), check
     * whether they've drifted out of the streaming radius, and advance
     * their fade-in/fade-out alpha.
     *
     * - A second pass, advanceIntensity(), recomputes every active,
     * non-retiring pattern's live intensity on a fast, shared cadence —
     * see WeatherBandStruct.getIntensityFor(). A pattern whose intensity
     * has decayed near zero is retired here, exactly like an out-of-range
     * or identity-mismatched pattern, through the same fade-out path.
     *
     * - A third, slower pass, advanceWeatherReevaluation(), periodically
     * re-resolves the weather at each pattern's own fixed home coordinate
     * on its own jittered cadence (reevaluationMinSeconds/MaxSeconds —
     * patterns never all recheck in the same frame) and retires the
     * pattern if its identity has changed, rather than ever swapping
     * weather/clouds in place on a fully-formed pattern.
     *
     * - Streaming a genuinely NEW pattern in — resolving its weather
     * band, building its lobes, allocating its struct — is capped at
     * maxPatternsStreamedPerFrame per frame AND at maxActivePatternCount
     * in total, walked across a precomputed, distance-sorted list of
     * candidate pattern-cell offsets relative to whichever pattern cell
     * the reference coordinate currently sits in.
     *
     * A pattern's rendered position (per lobe) is its fixed home center
     * plus accumulated wind drift plus that lobe's own fixed offset —
     * never re-centered or wrapped mid-life. Retirement is checked
     * against the pattern's home distance from the player, not its
     * drifted position — the same toroidal, wrap-safe distance math the
     * previous cell design already used (see wrapChunkCoordinate() and
     * WorldWrapUtility.wrappedDelta()) — a pattern's home chunk is always
     * wrapped into the active world's own bounds rather than rejected,
     * since the world is a torus for weather placement purposes exactly
     * like GlobalNoiseBranch and RegionSampleBranch already treat it for
     * weather noise sampling.
     *
     * Streaming radius is whichever is smaller of the player's configured
     * render distance (Settings.maxRenderDistance) and
     * EngineSetting.WEATHER_NEAR_RANGE_CHUNKS — see EngineSetting's own
     * doc comment — deliberately smaller than RegionSampleBranch's
     * far-range 8-direction sample distance, so real cloud geometry only
     * ever exists inside the range the sky-dome preview has already been
     * showing, and never past the edge of terrain that is actually being
     * drawn this session.
     */

    // Fade rates — alpha units per second
    private static final float FADE_IN_RATE = 0.6f;
    private static final float FADE_OUT_RATE = 0.6f;

    // Internal
    private WeatherManager weatherManager;
    private WorldManager worldManager;
    private WindManager windManager;

    // Settings
    private int patternCellSizeChunks;
    private float radiusChunks;
    private int maxPatternsStreamedPerFrame;
    private int maxActivePatternCount;
    private float driftScale;
    private float reevaluationMinSeconds;
    private float reevaluationMaxSeconds;

    // Registry — the persistent weather systems themselves.
    private Long2ObjectOpenHashMap<WeatherPatternStruct> activePatterns;

    // Registry — flattened per-lobe render view. CloudRenderSystem reads
    // this exactly as it always has, one OverheadCellStruct in, at most
    // one ModelInstance out. Maintained incrementally: populated when a
    // pattern streams in, pruned when a pattern fully retires — never
    // rebuilt from scratch, so an already-live lobe's own ModelInstance
    // is never needlessly recreated.
    private Long2ObjectOpenHashMap<OverheadCellStruct> activeCells;

    // Streaming — distance-sorted offsets (in pattern-cell units) relative
    // to the reference cell, walked round-robin across frames.
    private ObjectArrayList<int[]> candidateOffsets;
    private int scanCursor;

    // Weather Reevaluation — see advanceWeatherReevaluation(). A monotonic
    // simulation clock, advanced by deltaTime, purely for scheduling each
    // pattern's own slow recheck.
    private double elapsedSimTime;

    // Intensity — see advanceIntensity(). A plain shared accumulator,
    // deliberately not jittered per-pattern like elapsedSimTime's
    // reevaluation scheduling — intensity is safe, and desirable, to
    // recompute for every active pattern on the same tick.
    private float intensityUpdateAccumulator;

    // Scratch — reused every resolution call, never reallocated
    private final WeatherBandStruct bandScratch = new WeatherBandStruct();

    // Internal \\

    @Override
    protected void create() {

        // Settings
        this.patternCellSizeChunks = EngineSetting.WEATHER_PATTERN_CELL_SIZE_CHUNKS;
        // Real cloud objects never stream past the edge of actually-drawn
        // terrain — see the class doc comment above and
        // CloudRenderSystem.pushCloudSettings(), which derives
        // u_cloudHorizonDistance from this exact same expression.
        this.radiusChunks = Math.min(settings.maxRenderDistance, (float) EngineSetting.WEATHER_NEAR_RANGE_CHUNKS);
        this.maxPatternsStreamedPerFrame = EngineSetting.OVERHEAD_MAX_STREAM_PER_FRAME;
        this.maxActivePatternCount = EngineSetting.WEATHER_PATTERN_MAX_ACTIVE_COUNT;
        this.driftScale = EngineSetting.OVERHEAD_DRIFT_SPEED_SCALE;
        this.reevaluationMinSeconds = EngineSetting.WEATHER_PATTERN_REEVALUATION_INTERVAL_MIN_SECONDS;
        this.reevaluationMaxSeconds = EngineSetting.WEATHER_PATTERN_REEVALUATION_INTERVAL_MAX_SECONDS;

        // Registry
        this.activePatterns = new Long2ObjectOpenHashMap<>();
        this.activeCells = new Long2ObjectOpenHashMap<>();

        // Streaming
        this.candidateOffsets = buildCandidateOffsets();
        this.scanCursor = 0;

        // Weather Reevaluation
        this.elapsedSimTime = 0.0;

        // Intensity
        this.intensityUpdateAccumulator = 0f;
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

        int playerCellX = Math.floorDiv(playerChunkX, patternCellSizeChunks);
        int playerCellZ = Math.floorDiv(playerChunkZ, patternCellSizeChunks);

        advanceWindDrift();
        advanceWeatherReevaluation();
        advanceIntensity();
        advanceFadesAndRetire(playerChunkX, playerChunkZ);
        streamInBudgeted(playerCellX, playerCellZ);
    }

    // Candidate Offsets \\

    /*
     * Builds every (offsetX, offsetZ) pattern-cell offset — relative to
     * whichever pattern cell the reference coordinate currently occupies
     * — whose center falls within radiusChunks, sorted nearest-first so
     * newly streamed patterns always fill in from the player outward
     * rather than in scan order. A one-time cost at create().
     */
    private ObjectArrayList<int[]> buildCandidateOffsets() {

        int radiusCells = Math.max(1, Math.round(radiusChunks / (float) patternCellSizeChunks));

        ObjectArrayList<int[]> offsets = new ObjectArrayList<>();

        for (int ox = -radiusCells; ox <= radiusCells; ox++) {
            for (int oz = -radiusCells; oz <= radiusCells; oz++) {

                float worldOffsetX = ox * patternCellSizeChunks;
                float worldOffsetZ = oz * patternCellSizeChunks;
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

        if (activePatterns.size() >= maxActivePatternCount)
            return;

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

            // Displace the home off the cell's exact center — see the
            // class doc comment's "Home Jitter" section.
            int[] jitter = computeHomeJitter(patternKey);
            homeChunkX += jitter[0];
            homeChunkZ += jitter[1];

            long wrappedHome = wrapChunkCoordinate(homeChunkX, homeChunkZ);
            int wrappedHomeChunkX = Coordinate2Long.unpackX(wrappedHome);
            int wrappedHomeChunkZ = Coordinate2Long.unpackY(wrappedHome);

            streamInPattern(patternKey, wrappedHomeChunkX, wrappedHomeChunkZ);
            streamed++;
        }
    }

    /*
     * Displaces a pattern's home center within its own streaming cell by
     * up to EngineSetting.WEATHER_PATTERN_HOME_JITTER_RATIO of the cell's
     * own size, on each axis independently. Derived purely from
     * patternKey (itself a pure function of the cell coordinate — see
     * streamInBudgeted()), so the same cell always jitters the same way;
     * no extra state needs to be stored on WeatherPatternStruct for this.
     *
     * Without this, every pattern's home sat exactly at its cell's
     * center, so the capped 64 patterns visibly lined up on a perfect
     * grid the moment more than a couple were active at once — exactly
     * the "defined patch or grid" look the underlying weather noise field
     * itself was already built to avoid (see GlobalNoiseBranch/
     * RegionSampleBranch). This runs once, at stream-in, strictly after
     * the candidate cell itself has already been chosen — it only moves
     * the resulting pattern within that cell, never changes which cell a
     * given scan step lands on.
     */
    private int[] computeHomeJitter(long patternKey) {

        long jitterSeed = patternKey ^ 0x2545F4914F6CDD1DL;

        float jitterTX = hash01(jitterSeed);
        float jitterTZ = hash01(jitterSeed ^ 0x9E3779B97F4A7C15L);

        float jitterRangeChunks = patternCellSizeChunks * EngineSetting.WEATHER_PATTERN_HOME_JITTER_RATIO;

        int jitterX = Math.round((jitterTX - 0.5f) * jitterRangeChunks);
        int jitterZ = Math.round((jitterTZ - 0.5f) * jitterRangeChunks);

        return new int[] { jitterX, jitterZ };
    }

    /*
     * Streams in one new weather pattern at a fixed home coordinate,
     * resolving its persistent WeatherHandle identity through the
     * horizon-blended path (see the class doc comment), then building a
     * small, fixed set of lobes to approximate that weather's shape — see
     * WeatherPatternLobeStruct's own doc comment. Lobe count scales with
     * the resolved weather's own cloudCoverage
     * (WEATHER_PATTERN_LOBE_MIN_COUNT to WEATHER_PATTERN_LOBE_MAX_COUNT)
     * so a wispy fair-weather system reads as one or two sparse lobes and
     * a dense storm reads as a fuller, multi-lobe mass; a weather with no
     * clouds defined at all (hasClouds() == false) always gets a single,
     * cloudless lobe rather than wastefully allocating several — it will
     * never draw anything regardless of lobe count.
     *
     * Each lobe independently rolls whether it draws a cloud at all (the
     * same per-lobe coverage gate the old per-cell design already used),
     * and if so independently picks one of the weather's own chance-
     * weighted cloud entries — so one pattern can legitimately mix cloud
     * archetypes (a storm system with both a dense Nimbus core lobe and
     * thinner Stratus lobes trailing it) rather than every lobe sharing
     * an identical cloud choice.
     *
     * Every lobe offsets from the pattern's own home center by a random
     * angle and radius (radius forced to 0 for lobe 0, so a single-lobe
     * pattern always reads as centered on its own resolved coordinate
     * rather than drifting off to one side), scaled by
     * WEATHER_PATTERN_LOBE_SPREAD_RATIO of the pattern's own cell size.
     * All lobe-level randomness — coverage roll, cloud pick, seed, size
     * variance, domain rotation, offset angle/radius — is derived once
     * here from the pattern's own patternKey mixed with the lobe index,
     * never re-rolled for the pattern's lifetime, the same "persistent,
     * non-reblending identity" guarantee the old per-cell streaming
     * already gave.
     */
    private void streamInPattern(long patternKey, int homeChunkX, int homeChunkZ) {

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

        WeatherPatternStruct pattern = new WeatherPatternStruct(
                patternKey, homeChunkX, homeChunkZ, weatherHandle, lobes, driftSpeedScale, intensity);

        pattern.setNextReevaluationTime(elapsedSimTime + reevaluationIntervalFor(patternKey));

        activePatterns.put(patternKey, pattern);

        for (int i = 0; i < lobes.length; i++) {
            long lobeKey = computeLobeKey(patternKey, i);
            activeCells.put(lobeKey, new OverheadCellStruct(lobeKey, pattern, lobes[i]));
        }
    }

    /*
     * A pattern's own drift speed is the average CloudData.driftSpeedScale
     * of every one of its cloud-bearing lobes, falling back to a neutral
     * 1.0 if none drew a cloud (nothing rendered, so nothing to keep in
     * sync anyway). Resolved once here rather than per-lobe, since every
     * lobe of one pattern is meant to move together as a single weather
     * system — see the class doc comment.
     */
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

    /*
     * Wraps a chunk coordinate into the active world's own bounds — the
     * same toroidal treatment GlobalNoiseBranch and RegionSampleBranch
     * already give the weather noise field itself.
     */
    private long wrapChunkCoordinate(int chunkX, int chunkZ) {

        WorldHandle activeWorld = worldManager.getActiveWorld();
        int worldWidthChunks = activeWorld.getWorldScale().x / EngineSetting.CHUNK_SIZE;
        int worldHeightChunks = activeWorld.getWorldScale().y / EngineSetting.CHUNK_SIZE;

        int wrappedX = Math.floorMod(chunkX, worldWidthChunks);
        int wrappedZ = Math.floorMod(chunkZ, worldHeightChunks);

        return Coordinate2Long.pack(wrappedX, wrappedZ);
    }

    // Wind Drift \\

    /*
     * Advances every active pattern's drifted position by the same live
     * local wind vector every other wind-aware system reads — see
     * LocalWindBranch and RegionSampleBranch's own identical pattern.
     * driftScale (EngineSetting.OVERHEAD_DRIFT_SPEED_SCALE) is the
     * overhead layer's own chunks-per-second-per-unit-of-wind-speed
     * constant. Each pattern's own driftSpeedScale (see
     * WeatherPatternStruct.getDriftSpeedScale() /
     * computePatternDriftSpeedScale()) is applied on top of the shared
     * wind vector, so a pattern built mostly from a high, thin Stratus
     * lobe set and a pattern built mostly from a low, heavy Nimbus lobe
     * set still genuinely separate over time under the same wind.
     */
    private void advanceWindDrift() {

        Vector3 windDirection = windManager.getWindHandle().getLocalWindDirection();
        float windSpeed = windManager.getWindHandle().getLocalWindSpeed();
        float deltaTime = internal.getDeltaTime();

        double baseDeltaChunkX = windDirection.x * windSpeed * driftScale * deltaTime;
        double baseDeltaChunkZ = windDirection.z * windSpeed * driftScale * deltaTime;

        for (WeatherPatternStruct pattern : activePatterns.values()) {

            float patternDriftScale = pattern.getDriftSpeedScale();

            pattern.advanceDrift(baseDeltaChunkX * patternDriftScale, baseDeltaChunkZ * patternDriftScale);
        }
    }

    // Weather Reevaluation \\

    /*
     * Slowly re-checks each active, non-retiring pattern's weather
     * against the CURRENT active weather pool at that pattern's fixed
     * home coordinate, through the same horizon-blended resolution used
     * at stream-in (see the class doc comment). If the resolved primary
     * weather has changed since this pattern was last evaluated, the
     * pattern is retired exactly like a pattern that has drifted out of
     * streaming range — it fades out via the existing fade/retire
     * pipeline (advanceFadesAndRetire(), called right after this each
     * frame) and, once fully gone, the normal streamInBudgeted() pass is
     * free to fill that location again on a later frame.
     *
     * Deliberately does NOT swap the pattern's cloud/weather fields in
     * place — they are immutable for a pattern's lifetime by design (see
     * WeatherPatternStruct) — an instant swap would pop a fully-formed
     * weather system into a different type/altitude/color with no
     * transition.
     *
     * Each pattern's own recheck cadence is a fixed, per-pattern jittered
     * interval derived from its own patternKey — deliberately not
     * re-randomized every check, so a pattern's personal cadence stays
     * stable and patterns never happen to sync up with each other.
     */
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

    /*
     * Recomputes every active, non-retiring pattern's live weather
     * intensity on a fast, shared cadence — deliberately not the same
     * slow, per-pattern-jittered cadence identity uses (see
     * advanceWeatherReevaluation()). Resolves through the same horizon-
     * blended path (see the class doc comment) so a pattern's reported
     * intensity always agrees with the same resolution its identity was
     * built from, then scales the result by that weather's own
     * cloudCoverage exactly like the previous per-cell design already
     * did — see WeatherBandStruct.getIntensityFor()'s own doc comment for
     * why a caller holding a persistent identity must resolve intensity
     * for that specific handle rather than whichever weather the noise
     * currently favors.
     *
     * A pattern whose final intensity has decayed below
     * WEATHER_PATTERN_DISSIPATION_INTENSITY_THRESHOLD is retired here,
     * through the exact same fade-out path advanceFadesAndRetire()
     * already drives for an out-of-range or identity-mismatched pattern
     * — never swapped or revived in place.
     */
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

            float intensity = resolvePatternIntensity(bandScratch, pattern.getWeatherHandle());
            pattern.setIntensity(intensity);

            if (intensity <= EngineSetting.WEATHER_PATTERN_DISSIPATION_INTENSITY_THRESHOLD)
                pattern.setRetiring(true);
        }
    }

    /*
     * Resolves a specific weather handle's intensity within an already-
     * resolved band, then scales it by that weather's own cloudCoverage
     * — see advanceIntensity()'s doc comment for the full rationale.
     */
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

        // Same toroidal world dimensions wrapChunkCoordinate() wraps a
        // fresh pattern's home chunk into — a pattern streamed in across
        // the wrap seam must be measured against the player the same way,
        // or it reads as being on the far side of the map the instant it
        // spawns and immediately retires itself again.
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

    /*
     * Removes a fully-faded pattern and every one of its flattened lobe
     * entries from activeCells — lobe keys are deterministically
     * reconstructed from the pattern's own key and lobe count rather than
     * stored separately, since computeLobeKey() is a pure function of the
     * two.
     */
    private void removePattern(long patternKey) {

        WeatherPatternStruct pattern = activePatterns.remove(patternKey);

        if (pattern == null)
            return;

        for (int i = 0; i < pattern.getLobeCount(); i++)
            activeCells.remove(computeLobeKey(patternKey, i));
    }

    // Noise \\

    /*
     * Deterministic finalizer-style mix — same shape as
     * DayTrackerBranch.calculateRandomNoise() — turning a stable packed
     * seed into a uniform [0, 1) float. Never re-rolled for a given
     * pattern's/lobe's lifetime, which is exactly what makes a pattern's
     * weather identity and a lobe's own shape "persistent" rather than
     * reblending.
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

    /*
     * Mixes a pattern key with a lobe index into a well-distributed,
     * effectively-unique 64-bit key for that lobe's flattened entry in
     * activeCells — collision risk is negligible at the scale this system
     * ever operates at (at most WEATHER_PATTERN_MAX_ACTIVE_COUNT patterns
     * times a handful of lobes each).
     */
    private static long computeLobeKey(long patternKey, int lobeIndex) {

        long h = patternKey ^ (0x9E3779B97F4A7C15L * (lobeIndex + 1));
        h ^= (h >>> 33);
        h *= 0xff51afd7ed558ccdL;
        h ^= (h >>> 33);
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= (h >>> 33);

        return h;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    // Accessible \\

    /*
     * Live, flattened registry of active renderable cloud-volume lobes,
     * keyed by their own lobe key. Stage 2's instanced cloud renderer
     * (CloudRenderSystem) reads this directly each frame to keep its
     * per-cloud-type instance buffers in sync — treat as read-only; all
     * mutation happens here.
     */
    public Long2ObjectOpenHashMap<OverheadCellStruct> getActiveCells() {
        return activeCells;
    }

    public int getActiveCellCount() {
        return activeCells.size();
    }

    public int getActivePatternCount() {
        return activePatterns.size();
    }
}