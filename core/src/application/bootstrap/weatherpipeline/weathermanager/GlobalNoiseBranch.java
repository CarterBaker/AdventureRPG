package application.bootstrap.weatherpipeline.weathermanager;

import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;

class GlobalNoiseBranch extends BranchPackage {

    /*
     * Owns the planet's continuous rotation angle, its seasonal axial-tilt
     * latitude drift, a slower current-like meander wobble layered on top
     * of both, and the CPU-side global weather noise overlay driven by all
     * three. Rotation advances independently of the in-game calendar — a
     * flat rotationSpeed (degrees per real second) times deltaTime — so the
     * steady east-west scroll is a pure atmospheric-simulation concept, never
     * a calendar one. Tilt is the opposite: it deliberately rides the
     * calendar's own yearProgress, exactly like SkyColorBranch's seasonal
     * tint blend and CurrentTrackerBranch's sunrise/sunset shift, since a
     * planet's storm-track latitude drifting north and south over the year
     * IS the calendar's seasons acting on weather, not an independent clock.
     * Meander sits between the two on every axis that matters — see point 5
     * below — real, moment-to-moment motion, but far too slow for a player
     * to ever perceive as animating.
     *
     * Sampling works entirely in normalized, wrapped world-UV space rather
     * than raw chunk coordinates:
     *
     * 1. A chunk coordinate is divided by the world's own chunk-space width
     * and height (converted from WorldHandle's block-space scale) to get a
     * UV pair, then wrapped into [0, 1) — this is what makes the world a
     * torus for weather purposes, matching the fact that it wraps, and it
     * keeps every value that ever reaches a trig or noise function bounded
     * to a tiny, precision-safe range no matter how large the world is.
     * Previously this method rotated raw chunk coordinates (which can run
     * into the millions on a large world) directly through sin/cos in
     * float — float only carries ~7 significant digits, so at that
     * magnitude the fractional part the noise function needs was already
     * gone before rotation even started. It also mixed block-space
     * (WorldHandle.getWorldScale()) with chunk-space (the coordinate here)
     * without converting between them, which quietly placed the rotation
     * pivot CHUNK_SIZE times farther from the real map center than
     * intended. Both are fixed by this UV approach.
     *
     * 2. Instead of rotating the sample point around the map's center (which
     * barely moves a point near the center while sweeping a point near the
     * edge through many cells per rotation — a real asymmetry the old
     * design had), rotation drives a uniform east-west scroll of the UV
     * field instead. Every point on the map drifts at the same apparent
     * rate regardless of where it sits, which also mirrors how real
     * planetary rotation drives broadly zonal (east-west) atmospheric
     * banding rather than swirling everything around one fixed pole.
     *
     * 3. Axial tilt layers a second, independent drift on top of the U-axis
     * rotation: a wrapped shift of the V (north-south) sampling coordinate,
     * oscillating once per calendar year with Math.sin(yearProgress * 2π).
     * A perfectly upright world (axialTilt == 0) contributes nothing here —
     * the field degenerates back to the pre-tilt, pure east-west scroll.
     * A tilted world's storm bands instead visibly migrate toward one pole
     * during that hemisphere's summer and back during its winter, exactly
     * as axial tilt drives real seasonal storm-track migration. The tilt
     * angle itself (0-90 degrees, see WorldHandle.getAxialTilt()) is
     * normalized against 90° and then scaled by GLOBAL_WEATHER_TILT_INFLUENCE
     * so an Earth-like ~23° tilt produces a gentle drift, not a full pole-to-
     * pole sweep. Recomputed once per frame in advanceTilt() rather than
     * per sampleGlobalIntensity() call — yearProgress moves far too slowly
     * to need finer granularity than that.
     *
     * 4. The nominal cell size (GLOBAL_WEATHER_NOISE_CELL_SIZE) is rounded
     * to the nearest whole number of cells across the world's width and
     * height, and the noise hash lookup wraps cell indices with floorMod —
     * both necessary so the field tiles with zero seam at the wrap
     * boundary, rather than sampling two unrelated hash values on either
     * side of the seam.
     *
     * 5. Meander layers a third drift on top of rotation and tilt — a
     * traveling wave in the V (north-south) sampling coordinate, driven by
     * the SAME rotated U coordinate the sampleX below already uses (so the
     * wave pattern itself travels with the planet's own rotation rather
     * than sliding independently across it) plus its own much slower phase
     * clock (meanderPhase, advanced by advanceMeander() every frame). This
     * is what keeps the noise field reading as a genuine current — real
     * atmospheric jet streams and ocean currents meander into a handful of
     * standing crests and troughs around the globe
     * (GLOBAL_WEATHER_MEANDER_WAVE_NUMBER) rather than holding one static
     * latitude the way tilt alone would produce, and those meanders
     * themselves slowly reshape over many minutes rather than staying
     * rigid. GLOBAL_WEATHER_MEANDER_INFLUENCE keeps the wobble modest —
     * noticeably wavy without ever overpowering the seasonal tilt drift it
     * sits on top of.
     *
     * Queried directly by RegionSampleBranch, and exposed further by
     * WeatherManager for any other system (wind, horizon, overhead) that
     * wants it — always as a plain CPU value. Never pushed to a UBO on its
     * own; only the weather values it influences travel to the GPU, through
     * the existing WeatherData / WeatherRegionData UBOs.
     */

    // Internal
    private WorldManager worldManager;
    private ClockManager clockManager;

    // Settings
    private float noiseCellSize;
    private float globalInfluence;
    private float tiltInfluence;
    private int meanderWaveNumber;
    private float meanderInfluence;
    private float meanderPhaseSpeed;

    // Rotation
    private double rotationAngleDegrees;

    // Tilt — recomputed once per frame by advanceTilt(), reused by every
    // sampleGlobalIntensity() call that frame.
    private double latitudeShift;

    // Meander — recomputed once per frame by advanceMeander(), reused by
    // every sampleGlobalIntensity() call that frame exactly like
    // latitudeShift. See the class comment's "Meander" section for what
    // this represents.
    private double meanderPhase;

    // Internal \\

    @Override
    protected void create() {

        // Settings
        this.noiseCellSize = EngineSetting.GLOBAL_WEATHER_NOISE_CELL_SIZE;
        this.globalInfluence = EngineSetting.GLOBAL_WEATHER_INFLUENCE;
        this.tiltInfluence = EngineSetting.GLOBAL_WEATHER_TILT_INFLUENCE;
        this.meanderWaveNumber = EngineSetting.GLOBAL_WEATHER_MEANDER_WAVE_NUMBER;
        this.meanderInfluence = EngineSetting.GLOBAL_WEATHER_MEANDER_INFLUENCE;
        this.meanderPhaseSpeed = EngineSetting.GLOBAL_WEATHER_MEANDER_PHASE_SPEED;
    }

    @Override
    protected void get() {
        this.worldManager = get(WorldManager.class);
        this.clockManager = get(ClockManager.class);
    }

    @Override
    protected void update() {
        advanceRotation();
        advanceTilt();
        advanceMeander();
    }

    // Rotation \\

    private void advanceRotation() {

        WorldHandle activeWorld = worldManager.getActiveWorld();
        float rotationSpeed = activeWorld.getRotationSpeed();

        this.rotationAngleDegrees += rotationSpeed * internal.getDeltaTime();
        this.rotationAngleDegrees %= EngineSetting.DEGREES_PER_FULL_ROTATION;

        if (this.rotationAngleDegrees < 0)
            this.rotationAngleDegrees += EngineSetting.DEGREES_PER_FULL_ROTATION;
    }

    // Tilt \\

    /*
     * Derives this frame's seasonal north-south sampling shift from the
     * active world's axial tilt and the active calendar's current
     * yearProgress. See the class comment, point 3, for the full rationale.
     */
    private void advanceTilt() {

        float axialTiltDegrees = worldManager.getActiveWorld().getAxialTilt();
        double yearProgress = clockManager.getClockHandle().getYearProgress();

        double tiltFraction = axialTiltDegrees / 90.0;
        double tiltPhase = yearProgress * 2.0 * Math.PI;

        this.latitudeShift = Math.sin(tiltPhase) * tiltFraction * tiltInfluence;
    }

    // Meander \\

    /*
     * Advances the meander pattern's own slow phase clock — see the class
     * comment's "Meander" section. Unlike rotationAngleDegrees (which
     * literally IS the planet's rotation), this phase has no physical
     * meaning of its own beyond "how far the meander pattern has evolved
     * since world start" — it exists purely to keep the wave pattern
     * itself gradually reshaping, rather than settling into one static
     * meander shape that simply rotates rigidly with the planet for the
     * entire session.
     */
    private void advanceMeander() {

        this.meanderPhase += meanderPhaseSpeed * internal.getDeltaTime();
        this.meanderPhase %= (Math.PI * 2.0);
    }

    // Sampling \\

    /*
     * Samples the rotating, tilt-and-meander-drifting global noise field at
     * a world-space chunk coordinate, returning a coherent [0, 1] "global
     * storm intensity" value. See the class comment for why this works in
     * wrapped UV space rather than raw coordinates, and for what the tilt
     * and meander terms each contribute.
     */
    float sampleGlobalIntensity(long chunkCoordinate) {

        WorldHandle activeWorld = worldManager.getActiveWorld();

        double worldWidthChunks = activeWorld.getWorldScale().x / (double) EngineSetting.CHUNK_SIZE;
        double worldHeightChunks = activeWorld.getWorldScale().y / (double) EngineSetting.CHUNK_SIZE;

        int chunkX = Coordinate2Long.unpackX(chunkCoordinate);
        int chunkY = Coordinate2Long.unpackY(chunkCoordinate);

        double u = wrap01(chunkX / worldWidthChunks);
        double rotationProgress = rotationAngleDegrees / EngineSetting.DEGREES_PER_FULL_ROTATION;
        double rotatedU = wrap01(u + rotationProgress);

        // Meander rides the SAME rotated longitude sampleX below already
        // uses — see the class comment's "Meander" section — so the wave
        // pattern travels with the planet's own rotation instead of
        // sliding independently across it. meanderPhase then layers a
        // second, much slower reshaping on top, so the meander humps
        // themselves migrate and re-form over many minutes rather than
        // rotating past a sample point in perfect lockstep forever.
        double meanderShift = Math.sin(rotatedU * meanderWaveNumber * Math.PI * 2.0 + meanderPhase)
                * meanderInfluence;

        double v = wrap01(chunkY / worldHeightChunks + latitudeShift + meanderShift);

        int cellsX = (int) Math.max(1L, Math.round(worldWidthChunks / noiseCellSize));
        int cellsY = (int) Math.max(1L, Math.round(worldHeightChunks / noiseCellSize));

        double sampleX = rotatedU * cellsX;
        double sampleY = v * cellsY;

        return sampleNoise(sampleX, sampleY, cellsX, cellsY);
    }

    float getGlobalInfluence() {
        return globalInfluence;
    }

    double getRotationAngleDegrees() {
        return rotationAngleDegrees;
    }

    double getLatitudeShift() {
        return latitudeShift;
    }

    double getMeanderPhase() {
        return meanderPhase;
    }

    // Wrap \\

    private double wrap01(double value) {
        double wrapped = value % 1.0;
        return wrapped < 0 ? wrapped + 1.0 : wrapped;
    }

    // Noise \\

    /*
     * Coherent 2D value noise over a bounded [0, cellsX) x [0, cellsY) cell
     * grid, wrapping seamlessly at the edges via floorMod on the integer
     * cell indices. Identical hash/lerp/smoothstep shape to
     * RegionSampleBranch's local noise, with the hash term order swapped so
     * the two fields never correlate at the same coordinates.
     */
    private float sampleNoise(double sampleX, double sampleY, int cellsX, int cellsY) {

        int x0 = (int) Math.floor(sampleX);
        int y0 = (int) Math.floor(sampleY);

        float tx = (float) (sampleX - x0);
        float ty = (float) (sampleY - y0);

        int wrappedX0 = Math.floorMod(x0, cellsX);
        int wrappedY0 = Math.floorMod(y0, cellsY);
        int wrappedX1 = Math.floorMod(x0 + 1, cellsX);
        int wrappedY1 = Math.floorMod(y0 + 1, cellsY);

        float n00 = hash(wrappedX0, wrappedY0);
        float n10 = hash(wrappedX1, wrappedY0);
        float n01 = hash(wrappedX0, wrappedY1);
        float n11 = hash(wrappedX1, wrappedY1);

        float smoothTx = smoothstep(tx);
        float smoothTy = smoothstep(ty);

        float nx0 = lerp(n00, n10, smoothTx);
        float nx1 = lerp(n01, n11, smoothTx);

        return lerp(nx0, nx1, smoothTy);
    }

    private float hash(int x, int y) {

        int h = x * 668265263 + y * 374761393;
        h = (h ^ (h >>> 13)) * 1274126177;

        return ((h ^ (h >>> 16)) & 0x7fffffff) / (float) Integer.MAX_VALUE;
    }

    private float smoothstep(float t) {
        return t * t * (3f - 2f * t);
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}