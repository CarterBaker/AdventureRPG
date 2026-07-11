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
     * The same "no cloud drawn" outcome now also happens for a weather
     * that DOES define clouds but whose per-cell coverage roll misses —
     * see streamInCell()'s own doc comment.
     *
     * Streaming is split into four passes:
     *
     * - Every frame, ALL active cells get a cheap pass: advance their wind
     * drift, check whether they've drifted out of the streaming radius, and
     * advance their fade-in/fade-out alpha. This is plain float math —
     * with a few hundred to ~1000 active cells it costs nothing, so it is
     * never budgeted.
     *
     * - A second pass, advanceIntensity() — deliberately NOT the same slow,
     * jittered cadence advanceWeatherReevaluation() uses — recomputes every
     * active cell's live intensity (see OverheadCellStruct's own doc
     * comment and WeatherBandStruct.getIntensityFor()) on a fast, shared
     * cadence. This is what makes a streamed-in weather system visibly
     * strengthen and weaken from moment to moment, rather than only ever
     * existing at fixed full presence between the coarse identity checks
     * below. Intensity is resolved specifically FOR the cell's own frozen
     * weatherHandle — not for whichever weather the band's noise currently
     * favors — and then scaled by that weather's own cloudCoverage, so a
     * thin, low-coverage weather (a sunny day with only occasional puffs)
     * reads as genuinely wispy while a high-coverage weather (a storm)
     * reads as dense and near-total, rather than every weather rendering
     * at the same strength whenever its noise band happens to be pure.
     * A cell whose intensity has decayed near zero is retired here — through
     * the same fade-out path an out-of-range or identity-mismatched cell
     * uses — so a weather system that has genuinely weakened away
     * dissipates instead of silently reviving.
     *
     * - A third, slower pass — advanceWeatherReevaluation() — is what keeps
     * this whole system from reading as static once a cell has streamed in.
     * Every active, non-retiring cell periodically re-resolves the weather
     * at its own fixed home coordinate (the same resolveWeatherBand() call
     * used at stream-in) on its own jittered cadence
     * (EngineSetting.WEATHER_CELL_REEVALUATION_INTERVAL_MIN/MAX_SECONDS —
     * cells never all recheck in the same frame). If the resolved weather's
     * identity has changed, the cell is retired through the exact same
     * fade-out path a cell walking out of range uses — it is never swapped
     * in place — so a passing storm's cloud visibly dissipates rather than
     * instantly mutating into a different cloud type, and the vacated slot
     * is free to stream back in with whatever weather (and cloud, if any)
     * now actually resolves there. This is the piece that makes "a weather
     * is always active, but never static" true at the level of individual
     * physical clouds, not only at the level of the continuously-reblending
     * sky-dome samples RegionSampleBranch already produces.
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
     * cell that's already fading out — distance itself is measured the same
     * toroidal way a fresh cell's home chunk is resolved (see
     * wrapChunkCoordinate() and WorldWrapUtility.wrappedDelta()), so a cell
     * that streamed in by wrapping around one edge of the map never
     * misreads as being on the opposite side of the world from the player.
     * A cell's home chunk is always wrapped into the active world's own
     * bounds (derived from the world PNG's pixel dimensions — see
     * WorldBuilder) rather than rejected — this world is a torus for cloud
     * placement purposes, exactly like GlobalNoiseBranch and
     * RegionSampleBranch already treat it for weather noise sampling.
     * Previously, any candidate offset that pushed a cell's home chunk
     * outside [0, worldWidth) x [0, worldHeight) was silently skipped
     * instead of wrapped — which meant any offset carrying so much as one
     * negative axis near the world's chunk-space origin was permanently
     * unstreamable. Visually, the overhead cloud grid could only ever grow
     * toward positive X/Z from wherever the player happened to start, never
     * the other way, which is what read as "the cloud grid only exists in
     * one corner of the map."
     *
     * Streaming radius is whichever is smaller of the player's configured
     * render distance (Settings.maxRenderDistance) and
     * EngineSetting.WEATHER_NEAR_RANGE_CHUNKS (see EngineSetting's own doc
     * comment) — deliberately smaller than RegionSampleBranch's far-range
     * 8-direction sample distance, so real cloud geometry only ever exists
     * inside the range the sky-dome preview has already been showing, AND
     * never past the edge of terrain that is actually being drawn this
     * session. Render distance is almost always the smaller of the two in
     * practice, which is exactly the point — a real cloud object must never
     * be found floating over ground that was never rendered underneath it.
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
    private float reevaluationMinSeconds;
    private float reevaluationMaxSeconds;

    // Registry
    private Long2ObjectOpenHashMap<OverheadCellStruct> activeCells;

    // Streaming — distance-sorted offsets (in cells) relative to the
    // reference cell, walked round-robin across frames.
    private ObjectArrayList<int[]> candidateOffsets;
    private int scanCursor;

    // Weather Reevaluation — see advanceWeatherReevaluation(). A monotonic
    // simulation clock, advanced by deltaTime, purely for scheduling each
    // cell's own slow recheck.
    private double elapsedSimTime;

    // Intensity — see advanceIntensity(). A plain shared accumulator,
    // deliberately not jittered per-cell like elapsedSimTime's reevaluation
    // scheduling — intensity is safe, and desirable, to recompute for
    // every active cell on the same tick. See EngineSetting.
    // WEATHER_CELL_INTENSITY_UPDATE_INTERVAL_SECONDS's own doc comment.
    private float intensityUpdateAccumulator;

    // Scratch — reused every stream-in call, never reallocated
    private final WeatherBandStruct bandScratch = new WeatherBandStruct();

    // Internal \\

    @Override
    protected void create() {

        // Settings
        this.cellSizeChunks = EngineSetting.OVERHEAD_CELL_SIZE;
        // Real cloud objects never stream past the edge of actually-drawn
        // terrain — see the class doc comment above and
        // CloudRenderSystem.pushCloudSettings(), which derives
        // u_cloudHorizonDistance from this exact same expression.
        this.radiusChunks = Math.min(settings.maxRenderDistance, (float) EngineSetting.WEATHER_NEAR_RANGE_CHUNKS);
        this.maxStreamPerFrame = EngineSetting.OVERHEAD_MAX_STREAM_PER_FRAME;
        this.driftScale = EngineSetting.OVERHEAD_DRIFT_SPEED_SCALE;
        this.reevaluationMinSeconds = EngineSetting.WEATHER_CELL_REEVALUATION_INTERVAL_MIN_SECONDS;
        this.reevaluationMaxSeconds = EngineSetting.WEATHER_CELL_REEVALUATION_INTERVAL_MAX_SECONDS;

        // Registry
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

        int playerCellX = Math.floorDiv(playerChunkX, cellSizeChunks);
        int playerCellZ = Math.floorDiv(playerChunkZ, cellSizeChunks);

        advanceWindDrift();
        advanceWeatherReevaluation();
        advanceIntensity();
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

            long wrappedHome = wrapChunkCoordinate(homeChunkX, homeChunkZ);
            int wrappedHomeChunkX = Coordinate2Long.unpackX(wrappedHome);
            int wrappedHomeChunkZ = Coordinate2Long.unpackY(wrappedHome);

            streamInCell(cellKey, wrappedHomeChunkX, wrappedHomeChunkZ);
            streamed++;
        }
    }

    /*
     * Clear (or otherwise cloudless) cells still exist in OverheadManager's
     * registry so their patch of weather stays trackable — see
     * OverheadManager's own doc comment — but there is nothing to render
     * here, and no ModelInstance is ever created for one. A cell's cloud
     * choice is fixed for its entire lifetime (see OverheadCellStruct), so
     * once an instance exists its archetype-level uniforms never need
     * re-baking — only position/fade/intensity are refreshed below.
     */
    private void streamInCell(long cellKey, int homeChunkX, int homeChunkZ) {

        long chunkCoordinate = Coordinate2Long.pack(homeChunkX, homeChunkZ);
        weatherManager.resolveWeatherBand(bandScratch, chunkCoordinate);

        WeatherHandle weatherHandle = bandScratch.getPrimary();
        // See advanceIntensity()'s own doc comment for the full rationale
        // behind resolveCellIntensity() over a bare getPrimaryIntensity()
        // call. At this exact moment weatherHandle was JUST set to
        // getPrimary(), so the two are numerically identical here — but
        // routing through the shared helper keeps this call site and
        // advanceIntensity()'s recompute call site permanently in
        // agreement, rather than silently diverging later.
        float intensity = resolveCellIntensity(bandScratch, weatherHandle);

        // Coverage gate — decides whether THIS cell's patch of sky
        // actually holds a cloud at all, independently of which cloud
        // type it would pick if it does. Previously every cell whose
        // weather defined any cloud entries streamed in with a visible
        // cloud (most weathers give at least one entry a 100% relative
        // chance, so nearly every cell — sunny, stormy, or anything in
        // between — ended up drawing one), which is what read as a solid
        // grid of boxes regardless of how overcast the weather actually
        // was. cloudCoverage is the value every weather already defines
        // for exactly this purpose (see WeatherData/WeatherHandle) but
        // this streaming pass never actually consulted it — a 0.15
        // Sunny sky now leaves roughly 85% of its cells genuinely bare, a
        // 0.9 Stormy sky leaves roughly 90% of its cells covered, and a
        // Clear sky (cloudCoverage 0.0, no cloud entries besides) stays
        // exactly as bare as it always was. This roll is independent of
        // cloudPickNoise below so "covered or not" and "which cloud type"
        // never correlate with each other.
        float coveragePickNoise = hash01(cellKey ^ 0xA24BAED4963EE407L);
        boolean coveredByCloud = weatherHandle.hasClouds()
                && coveragePickNoise < weatherHandle.getCloudCoverage();

        float cloudPickNoise = hash01(cellKey);
        CloudChanceStruct cloudEntry = coveredByCloud ? weatherHandle.pickCloud(cloudPickNoise) : null;
        float randomSeed = hash01(cellKey ^ 0x9E3779B97F4A7C15L);

        // A Clear weather, a weather whose coverage roll missed, or any
        // other cloudless resolution all land here identically — the cell
        // still streams in and carries its WeatherHandle identity, it
        // simply has nothing to draw. See the class doc comment.
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
                randomSeed,
                intensity);

        cell.setNextReevaluationTime(elapsedSimTime + reevaluationIntervalFor(cellKey));

        activeCells.put(cellKey, cell);
    }

    /*
     * Wraps a chunk coordinate into the active world's own bounds — the
     * same toroidal treatment GlobalNoiseBranch and RegionSampleBranch
     * already give the weather noise field itself (see their own doc
     * comments on why the world is a torus for weather purposes). World
     * scale is stored in BLOCKS (see WorldBuilder.calculateWorldScale()),
     * so it is converted to chunk units before wrapping — the same
     * conversion this class already performed when this method used to be
     * a bounds check instead of a wrap.
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
     * Advances every active cell's drifted position by the same live local
     * wind vector every other wind-aware system reads — see
     * LocalWindBranch and RegionSampleBranch's own identical pattern.
     * driftScale (EngineSetting.OVERHEAD_DRIFT_SPEED_SCALE) is the overhead
     * grid's own chunks-per-second-per-unit-of-wind-speed constant, kept
     * separate from RegionSampleBranch's WEATHER_WIND_DRIFT_SCALE so the
     * two can be tuned independently (visible cloud objects drifting at a
     * different apparent rate than the CPU noise field driving them is
     * fine — they're not required to stay pixel-locked to each other).
     *
     * Each cell's own CloudHandle.getDriftSpeedScale() is applied on top of
     * the shared wind vector — previously every cell drifted at the exact
     * same rate regardless of cloud type, leaving CloudData.driftSpeedScale
     * entirely unread. A high, thin Stratus and a low, heavy Nimbus now
     * genuinely separate over time under the same wind instead of marching
     * across the sky in lockstep. Cloudless cells (Clear weather) have no
     * CloudHandle to read a scale from, so they fall back to 1.0 — they
     * have nothing rendered to visibly drift anyway, but keeping the drift
     * accumulator advancing at a neutral rate avoids any special-cased
     * discontinuity if that cell's weather later resolves to a cloud-
     * bearing one without ever restarting its drift from zero.
     */
    private void advanceWindDrift() {

        Vector3 windDirection = windManager.getWindHandle().getLocalWindDirection();
        float windSpeed = windManager.getWindHandle().getLocalWindSpeed();
        float deltaTime = internal.getDeltaTime();

        double baseDeltaChunkX = windDirection.x * windSpeed * driftScale * deltaTime;
        double baseDeltaChunkZ = windDirection.z * windSpeed * driftScale * deltaTime;

        for (OverheadCellStruct cell : activeCells.values()) {

            float cellDriftScale = cell.hasCloud() ? cell.getCloudHandle().getDriftSpeedScale() : 1f;

            cell.advanceDrift(baseDeltaChunkX * cellDriftScale, baseDeltaChunkZ * cellDriftScale);
        }
    }

    // Weather Reevaluation \\

    /*
     * Slowly re-checks each active, non-retiring cell's weather against the
     * CURRENT active weather pool at that cell's fixed home coordinate — the
     * same resolution WeatherManager already performs for the reference
     * region and for a cell's initial stream-in (see streamInCell() and
     * WeatherManager.resolveWeatherBand()). If the resolved primary weather
     * has changed since this cell was last evaluated, the cell is retired
     * exactly like a cell that has drifted out of streaming range — it fades
     * out via the existing fade/retire pipeline (advanceFadesAndRetire(),
     * called right after this each frame) and, once fully gone, the normal
     * streamInBudgeted() pass is free to fill that location again on a
     * later frame, at which point it picks up whatever weather now resolves
     * there. This is what makes "a weather is always active, but never
     * static" true at the level of individual physical clouds, not just the
     * continuously-reblending sky-dome samples RegionSampleBranch already
     * produces — without this, a cloud streamed in under a passing storm
     * would keep that storm's cloud type forever, even long after the
     * storm noise had moved on, until the player wandered far enough away
     * to force a restream.
     *
     * Deliberately does NOT swap the cell's cloud/weather fields in place —
     * they are immutable for a cell's lifetime by design (see
     * OverheadCellStruct) — an instant swap would pop a fully-formed cloud
     * into a different type/altitude/color with no transition. Fading the
     * old cloud out and letting a fresh cell fade a new one in nearby reads
     * as actual weather change — a storm cloud dissipating, a fair-weather
     * cloud forming — rather than a glitch.
     *
     * Each cell's own recheck cadence is a fixed, per-cell jittered interval
     * derived from its own cellKey (same hashing approach used for its
     * cloud pick and random seed) — deliberately not re-randomized every
     * check, so a cell's personal cadence stays stable and cells never
     * happen to sync up with each other.
     */
    private void advanceWeatherReevaluation() {

        elapsedSimTime += internal.getDeltaTime();

        for (OverheadCellStruct cell : activeCells.values()) {

            if (cell.isRetiring())
                continue;

            if (elapsedSimTime < cell.getNextReevaluationTime())
                continue;

            long homeCoordinate = Coordinate2Long.pack(cell.getHomeChunkX(), cell.getHomeChunkZ());
            weatherManager.resolveWeatherBand(bandScratch, homeCoordinate);

            if (bandScratch.getPrimary() != cell.getWeatherHandle())
                cell.setRetiring(true);
            else
                cell.setNextReevaluationTime(elapsedSimTime + reevaluationIntervalFor(cell.getCellKey()));
        }
    }

    // Intensity \\

    /*
     * Recomputes every active, non-retiring cell's live weather intensity
     * on a fast, shared cadence — deliberately not the same slow, per-cell-
     * jittered cadence identity uses (see advanceWeatherReevaluation()).
     *
     * Resolves intensity specifically for THIS CELL'S OWN frozen
     * weatherHandle via WeatherBandStruct.getIntensityFor(), not via
     * getPrimaryIntensity(). An earlier version of this method called
     * getPrimaryIntensity() directly, which describes whichever weather
     * the band's noise currently favors — not necessarily the weather this
     * cell actually committed to at stream-in. Once a cell's identity has
     * drifted out of "primary" (the noise has moved on to favor a
     * neighboring weather, but this cell's own slow, jittered reevaluation
     * hasn't yet caught up to notice — see advanceWeatherReevaluation()),
     * that older code would silently report the NEW, rising neighbor's
     * intensity as if it belonged to this cell, so a cell whose weather
     * should already be fading could instead appear to hold steady or even
     * strengthen for up to a full reevaluation interval. getIntensityFor()
     * fixes this by always measuring the specific handle passed in,
     * correctly reading as near-zero the moment this cell's own identity
     * stops being favored, regardless of what the noise has moved on to.
     *
     * The result is then scaled by that weather's own cloudCoverage (see
     * WeatherData/WeatherHandle) — a purely noise-purity intensity treats
     * every weather as equally "thick" whenever its band is pure, which
     * made a wispy, low-coverage weather (a sunny day's occasional puffs)
     * read exactly as dense as a high-coverage storm. Multiplying by
     * coverage is what makes light weather actually look light and heavy
     * weather actually look heavy.
     *
     * A cell whose final intensity has decayed below
     * WEATHER_CELL_DISSIPATION_INTENSITY_THRESHOLD is retired here, through
     * the exact same fade-out path advanceFadesAndRetire() already drives for
     * an out-of-range or identity-mismatched cell — never swapped or revived
     * in place. This is deliberately not a special case: the triangle-shaped
     * intensity curve already dips to zero at exactly the moment a resolved
     * band's identity would flip, so a low-intensity cell is already
     * visually faded down before it would otherwise go stale, and letting it
     * fully retire there rather than revive is what makes weather genuinely
     * dissipate over time instead of only ever changing because the player
     * wandered out of range.
     */
    private void advanceIntensity() {

        intensityUpdateAccumulator += internal.getDeltaTime();

        if (intensityUpdateAccumulator < EngineSetting.WEATHER_CELL_INTENSITY_UPDATE_INTERVAL_SECONDS)
            return;

        intensityUpdateAccumulator = 0f;

        for (OverheadCellStruct cell : activeCells.values()) {

            if (cell.isRetiring())
                continue;

            long homeCoordinate = Coordinate2Long.pack(cell.getHomeChunkX(), cell.getHomeChunkZ());
            weatherManager.resolveWeatherBand(bandScratch, homeCoordinate);

            float intensity = resolveCellIntensity(bandScratch, cell.getWeatherHandle());
            cell.setIntensity(intensity);

            if (intensity <= EngineSetting.WEATHER_CELL_DISSIPATION_INTENSITY_THRESHOLD)
                cell.setRetiring(true);
        }
    }

    /*
     * Resolves a specific weather handle's intensity within an already-
     * resolved band, then scales it by that weather's own cloudCoverage.
     * See advanceIntensity()'s doc comment for the full rationale — in
     * short: WeatherBandStruct.getIntensityFor(handle) rather than
     * getPrimaryIntensity() so a cell's reported intensity always tracks
     * its OWN identity rather than whichever weather the noise currently
     * favors, and the cloudCoverage scale so a thin weather renders thin
     * and a thick weather renders thick, rather than every weather looking
     * equally dense whenever its own band happens to be pure.
     */
    private float resolveCellIntensity(WeatherBandStruct band, WeatherHandle handle) {
        return band.getIntensityFor(handle) * handle.getCloudCoverage();
    }

    private float reevaluationIntervalFor(long cellKey) {

        float t = hash01(cellKey ^ 0xD1B54A32D192ED03L);

        return lerp(reevaluationMinSeconds, reevaluationMaxSeconds, t);
    }

    // Fade / Retire \\

    private void advanceFadesAndRetire(int playerChunkX, int playerChunkZ) {

        float deltaTime = internal.getDeltaTime();
        LongArrayList toRemove = null;

        // Same toroidal world dimensions wrapChunkCoordinate() wraps a
        // fresh cell's home chunk into — a cell streamed in across the
        // wrap seam must be measured against the player the same way, or
        // it reads as being on the far side of the map the instant it
        // spawns and immediately retires itself again. See
        // WorldWrapUtility.wrappedDelta() — the same canonical correction
        // CloudRenderSystem now uses to resolve each cell's render
        // position, so a cell's retirement distance and its rendered
        // position can never disagree about which side of the seam it's
        // really on.
        WorldHandle activeWorld = worldManager.getActiveWorld();
        int worldWidthChunks = activeWorld.getWorldScale().x / EngineSetting.CHUNK_SIZE;
        int worldHeightChunks = activeWorld.getWorldScale().y / EngineSetting.CHUNK_SIZE;

        for (OverheadCellStruct cell : activeCells.values()) {

            double dx = WorldWrapUtility.wrappedDelta(cell.getHomeChunkX(), playerChunkX, worldWidthChunks);
            double dz = WorldWrapUtility.wrappedDelta(cell.getHomeChunkZ(), playerChunkZ, worldHeightChunks);
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

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
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