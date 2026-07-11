// WeatherManager.java
package application.bootstrap.weatherpipeline.weathermanager;

import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.weatherpipeline.seasonmanager.SeasonManager;
import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import application.bootstrap.worldpipeline.biome.BiomeHandle;
import application.bootstrap.worldpipeline.biome.WeatherChanceStruct;
import application.bootstrap.worldpipeline.biomemanager.BiomeManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.vectors.Vector3;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2ShortOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;

public class WeatherManager extends ManagerPackage {

    /*
     * Owns the weather palette for the engine lifetime and drives the live
     * weather simulation. Supports on-demand loading via InternalLoader on a
     * cache miss, keyed by weather name (e.g. "standard/Sunny"). The active
     * seasonal pool is re-resolved only when the season changes — season
     * identity now comes from the active calendar (see
     * ClockHandle.getCurrentSeason()) rather than a fixed enum, so biomes
     * can define weather pools for whatever named seasons their world's
     * calendar uses. Per-frame region sampling, the planet-rotation-driven
     * global noise overlay, live temperature, and the GPU UBO pushes are
     * delegated to branches.
     *
     * Weather is the fusion of three inputs, combined once per frame in
     * RegionSampleBranch: which biome is active (resolveActiveBiome()),
     * which named season that biome is currently in (the calendar, via
     * ClockHandle.getCurrentSeason()), and the world's global, rotating
     * weather noise (GlobalNoiseBranch — the "separate virtual noise map
     * wrapped around the planet" — blended with a wind-drifted local noise
     * layer). Neither wind nor weather is a one-way input to the other:
     * wind drags the local noise sampling point (see
     * RegionSampleBranch.advanceWindDrift()), so weather visibly travels
     * with the wind, while the currently resolved weather's own
     * windSpeedScale/windTurbulenceScale feed back into LocalWindBranch,
     * making wind gustier and more erratic under a storm than under a
     * clear sky. See LocalWindBranch's own doc comment for the wind side
     * of this loop.
     *
     * The active season contributes more than just its name as a lookup
     * key, too — resolveWeatherPool() reads that season's own
     * precipitationChanceScale (SeasonData/SeasonHandle) and uses it to
     * bias the chance weight of every precipitating weather in the
     * biome's pool for that season, so "biome + season + noise" really
     * does fold in the season's own climate numbers, not just its name.
     *
     * updateReferenceCoordinate() resolves the main window's player world
     * position into a chunk coordinate every frame, before any sampling
     * happens this frame, and writes it into RegionSampleBranch via
     * setReferenceCoordinate() — this is the "grab the data for the part of
     * the world we're actually standing in" hookup. Falls back to whatever
     * the reference coordinate already was (default origin) if there is no
     * main window or no spawned player yet — normal before a player exists.
     *
     * getReferenceCoordinate() — the INTEGER chunk-granular coordinate — is
     * the single reference position both OverheadManager's cell streaming
     * and CloudRenderSystem's render-space positioning use. Every
     * moving-world offset elsewhere in the engine (terrain's own
     * u_gridPosition, see GridBuildSystem; world items' u_playerChunkX/
     * u_playerChunkZ, see StandardItemShader.vsh) is anchored to this same
     * kind of integer chunk coordinate, never a continuous sub-chunk
     * position — the camera's own view/projection matrix already carries
     * the player's continuous motion within their current chunk, so
     * nothing downstream needs to reapply it. An earlier revision of this
     * manager additionally tracked the player's raw continuous world
     * position for CloudRenderSystem to offset cloud instances by,
     * reasoning that the integer chunk coordinate alone left a cloud's
     * render offset "frozen" for as long as the player stayed inside one
     * chunk. That double-counted the player's own sub-chunk motion on top
     * of what the camera's view transform already applies, and was the
     * actual source of the "clouds shift slightly when the player wraps
     * inside a chunk" bug rather than a fix for it — see
     * CloudRenderSystem.updateInstances()'s own doc comment for the full
     * account. That continuous tracking has been removed; the integer
     * coordinate below is now the only reference position this manager
     * exposes.
     *
     * activeWeatherPool stays null until the calendar resolves its first
     * named season, which requires at least one day-tick — normal at
     * startup. update() skips sampling entirely until then, and any
     * cross-package caller of resolveWeatherBand() should check
     * hasActiveWeatherPool() first; calling it before that point throws
     * rather than silently resolving against nothing. getWindSpeedScale(),
     * getWindTurbulenceScale(), getHumidity(), and getVisibility() are the
     * exceptions — all four fall back to a neutral value before that point
     * instead of throwing or silently reading an unset zero, so nothing
     * downstream (wind, fog, gameplay) goes to a dead/degenerate state
     * during the first frames of a session.
     */

    // Internal
    private ClockManager clockManager;
    private BiomeManager biomeManager;
    private WindowManager windowManager;
    private PlayerManager playerManager;
    private SeasonManager seasonManager;

    // Branches
    private GlobalNoiseBranch globalNoiseBranch;
    private RegionSampleBranch regionSampleBranch;
    private TemperatureBranch temperatureBranch;
    private WeatherBufferBranch internalBuffer;

    // Palette
    private Object2ShortOpenHashMap<String> weatherName2WeatherID;
    private Short2ObjectOpenHashMap<WeatherHandle> weatherID2WeatherHandle;

    // Season Tracking
    private String lastSeason;
    private ObjectArrayList<WeatherPoolEntryStruct> activeWeatherPool;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.weatherName2WeatherID = new Object2ShortOpenHashMap<>();
        this.weatherID2WeatherHandle = new Short2ObjectOpenHashMap<>();

        // Branches
        this.globalNoiseBranch = create(GlobalNoiseBranch.class);
        this.regionSampleBranch = create(RegionSampleBranch.class);
        this.temperatureBranch = create(TemperatureBranch.class);
        this.internalBuffer = create(WeatherBufferBranch.class);

        create(WeatherLoader.class);
    }

    @Override
    protected void get() {

        // Internal
        this.clockManager = get(ClockManager.class);
        this.biomeManager = get(BiomeManager.class);
        this.windowManager = get(WindowManager.class);
        this.playerManager = get(PlayerManager.class);
        this.seasonManager = get(SeasonManager.class);
    }

    @Override
    protected void awake() {
        this.internalBuffer.assignData(regionSampleBranch);
    }

    @Override
    protected void update() {

        updateReferenceCoordinate();

        String currentSeason = clockManager.getClockHandle().getCurrentSeason();

        if (currentSeason != null && !currentSeason.equals(lastSeason)) {
            BiomeHandle activeBiome = resolveActiveBiome();
            this.activeWeatherPool = resolveWeatherPool(activeBiome, currentSeason);
            this.lastSeason = currentSeason;
        }

        if (activeWeatherPool == null)
            return;

        regionSampleBranch.sampleRegions(activeWeatherPool);
        temperatureBranch.updateTemperature(regionSampleBranch.getCenterSample());
    }

    // Reference Coordinate \\

    /*
     * Resolves the main window's player position into the chunk coordinate
     * every downstream system treats as "here" — the single reference
     * position both weather sampling and cloud/overhead-cell rendering
     * anchor against. A no-op if there is no main window or no player has
     * spawned into it yet, leaving the reference at whatever it last
     * resolved to (origin by default).
     */
    private void updateReferenceCoordinate() {

        WindowInstance mainWindow = windowManager.getMainWindow();

        if (mainWindow == null)
            return;

        int windowID = mainWindow.getWindowID();

        if (!playerManager.hasPlayerForWindow(windowID))
            return;

        EntityInstance player = playerManager.getPlayerForWindow(windowID);
        Vector3 position = player.getWorldPositionStruct().getPosition();

        int chunkX = Math.floorDiv((int) Math.floor(position.x), EngineSetting.CHUNK_SIZE);
        int chunkZ = Math.floorDiv((int) Math.floor(position.z), EngineSetting.CHUNK_SIZE);

        setReferenceCoordinate(Coordinate2Long.pack(chunkX, chunkZ));
    }

    // Biome Selection \\

    /*
     * Resolves which biome supplies the active seasonal weather pool.
     * Single hardcoded biome for now (EngineSetting.DEFAULT_BIOME_NAME) —
     * this is the one integration point a future per-location biome lookup
     * (sampling a biome map at the current reference coordinate, the same
     * way the world generator assigns biomes per block) needs to replace.
     * Kept as its own method precisely so that hookup is a one-line change
     * here rather than a change to update()'s control flow or a second
     * biome-resolution path growing somewhere else.
     */
    private BiomeHandle resolveActiveBiome() {
        return biomeManager.getBiomeHandleFromBiomeName(EngineSetting.DEFAULT_BIOME_NAME);
    }

    // Management \\

    void addWeatherHandle(WeatherHandle weatherHandle) {

        if (weatherID2WeatherHandle.containsKey(weatherHandle.getWeatherID())) {
            WeatherHandle existing = weatherID2WeatherHandle.get(weatherHandle.getWeatherID());
            if (RegistryUtility.isCollision(weatherHandle.getWeatherName(), existing.getWeatherName(),
                    weatherHandle.getWeatherID()))
                throwException("Weather ID collision: '"
                        + weatherHandle.getWeatherName() + "' collides with '"
                        + existing.getWeatherName() + "' (ID " + weatherHandle.getWeatherID()
                        + ") — rename one weather to resolve");
        }

        weatherName2WeatherID.put(weatherHandle.getWeatherName(), weatherHandle.getWeatherID());
        weatherID2WeatherHandle.put(weatherHandle.getWeatherID(), weatherHandle);
    }

    // On-Demand \\

    public void request(String weatherName) {
        ((WeatherLoader) internalLoader).request(weatherName);
    }

    // Biome Resolution \\

    /*
     * Resolves a biome's named-season weather chance entries into live
     * handles, preserving JSON declaration order — RegionSampleBranch blends
     * across this pool by chance-weighted band, not array position, so no
     * sort is needed here. Falls back via resolveFallbackEntries() when the
     * biome has no entries for this exact season name — see that method.
     *
     * Every entry whose weather actually precipitates additionally has its
     * chance weight scaled by the active season's own precipitationChanceScale
     * (see resolvePrecipitationBias()) — this is what makes the active
     * SEASON DATA, not just the season name, part of weather resolution.
     */
    private ObjectArrayList<WeatherPoolEntryStruct> resolveWeatherPool(BiomeHandle biomeHandle, String season) {

        ObjectArrayList<WeatherChanceStruct> entries = biomeHandle.getWeatherEntriesForSeason(season);

        if (entries.isEmpty())
            entries = resolveFallbackEntries(biomeHandle, season);

        float precipitationBias = resolvePrecipitationBias(season);

        ObjectArrayList<WeatherPoolEntryStruct> pool = new ObjectArrayList<>(entries.size());

        for (int i = 0; i < entries.size(); i++) {

            WeatherChanceStruct entry = entries.get(i);
            WeatherHandle handle = getWeatherHandleFromWeatherName(entry.getWeatherName());
            float chance = entry.getChance();

            if (handle.getPrecipitationIntensity() > 0f)
                chance *= precipitationBias;

            pool.add(new WeatherPoolEntryStruct(handle, chance));
        }

        return pool;
    }

    /*
     * Resolves the active season's own precipitationChanceScale (a per-
     * season climate multiplier — see SeasonData) so wetter seasons
     * visibly skew their biome's pool toward precipitating weathers and
     * drier seasons skew away from them. Deliberately does not guard
     * against a missing SeasonHandle the way resolveFallbackEntries()
     * guards against a missing biome pool — LocalWindBranch and
     * TemperatureBranch already assume every calendar season name has a
     * companion season climate file and resolve it unguarded, so a
     * genuinely missing file is a data error that should surface the same
     * way here as it already does for wind and temperature.
     */
    private float resolvePrecipitationBias(String season) {
        return seasonManager.getSeasonHandleFromSeasonName(season).getPrecipitationChanceScale();
    }

    /*
     * Called only when the biome has no "weathers" entry for the calendar's
     * exact current season name — a mismatched or renamed season, not
     * necessarily a malformed biome (one biome file may be shared across
     * worlds running different calendars with different season names).
     * Logs a clear warning naming both the missing season and the biome's
     * actual defined seasons, then falls back to the first season the biome
     * DID define (JSON declaration order) so an unexpected season name
     * degrades to "wrong-but-plausible weather" instead of crashing the
     * engine outright the first time that season becomes active — which,
     * for a rare or custom-calendar season, could be arbitrarily far into a
     * session. A biome with literally no "weathers" block at all is a
     * genuine data error and still throws — there is nothing sensible to
     * fall back to.
     */
    private ObjectArrayList<WeatherChanceStruct> resolveFallbackEntries(BiomeHandle biomeHandle, String season) {

        if (!biomeHandle.hasAnyWeathers())
            throwException("Biome \"" + biomeHandle.getBiomeName() +
                    "\" has no \"weathers\" block defined at all — cannot resolve any season, including \""
                    + season + "\"");

        String fallbackSeason = biomeHandle.getDefinedSeasonNames().get(0);

        errorLog("[WeatherManager] Biome \"" + biomeHandle.getBiomeName() +
                "\" has no weathers defined for season \"" + season + "\" — falling back to \"" +
                fallbackSeason + "\". Defined seasons: " + biomeHandle.getDefinedSeasonNames());

        return biomeHandle.getWeatherEntriesForSeason(fallbackSeason);
    }

    // Accessible \\

    public boolean hasWeather(String weatherName) {
        return weatherName2WeatherID.containsKey(weatherName);
    }

    public short getWeatherIDFromWeatherName(String weatherName) {

        if (!weatherName2WeatherID.containsKey(weatherName))
            request(weatherName);

        return RegistryUtility.toShortID(weatherName);
    }

    public WeatherHandle getWeatherHandleFromWeatherID(short weatherID) {

        WeatherHandle handle = weatherID2WeatherHandle.get(weatherID);

        if (handle == null)
            throwException("No handle registered for weather ID: " + weatherID);

        return handle;
    }

    public WeatherHandle getWeatherHandleFromWeatherName(String weatherName) {
        return getWeatherHandleFromWeatherID(getWeatherIDFromWeatherName(weatherName));
    }

    public void setReferenceCoordinate(long chunkCoordinate) {
        regionSampleBranch.setReferenceCoordinate(chunkCoordinate);
    }

    public long getReferenceCoordinate() {
        return regionSampleBranch.getReferenceCoordinate();
    }

    /*
     * Whether a seasonal weather pool has ever been resolved. False for the
     * first frame(s) of a session, before the calendar's first day-tick —
     * see the class comment.
     */
    public boolean hasActiveWeatherPool() {
        return activeWeatherPool != null;
    }

    /*
     * Exposes the reference region's blended windSpeedScale for WindManager's
     * LocalWindBranch, without leaking the package-private WeatherSampleStruct.
     * Falls back to a neutral 1.0 (EngineSetting.DEFAULT_WEATHER_WIND_SPEED_SCALE)
     * before the first weather pool ever resolves, rather than reading
     * WeatherSampleStruct's unset default of 0.0 and going dead calm.
     */
    public float getWindSpeedScale() {

        if (!hasActiveWeatherPool())
            return EngineSetting.DEFAULT_WEATHER_WIND_SPEED_SCALE;

        return regionSampleBranch.getCenterSample().getWindSpeedScale();
    }

    /*
     * Exposes the reference region's blended windTurbulenceScale for
     * WindManager's LocalWindBranch — mirrors getWindSpeedScale() exactly,
     * just scaling gust amplitude and direction wobble instead of base
     * speed. Same neutral 1.0 fallback before any weather pool has resolved.
     */
    public float getWindTurbulenceScale() {

        if (!hasActiveWeatherPool())
            return EngineSetting.DEFAULT_WEATHER_WIND_TURBULENCE_SCALE;

        return regionSampleBranch.getCenterSample().getWindTurbulenceScale();
    }

    /*
     * Exposes the reference region's blended humidity — completes the full
     * Weather property set (precipitation, wind, humidity, visibility) at
     * the resolved-region level; previously parsed per-Weather but never
     * carried through sampling. No renderer consumes this yet — it's
     * available for future gameplay systems (crop growth, item spoilage,
     * etc.) the same way getCurrentTemperature() already is. Falls back to
     * a neutral 0.5 (EngineSetting.DEFAULT_WEATHER_HUMIDITY) before the
     * first weather pool ever resolves.
     */
    public float getHumidity() {

        if (!hasActiveWeatherPool())
            return EngineSetting.DEFAULT_WEATHER_HUMIDITY;

        return regionSampleBranch.getCenterSample().getHumidity();
    }

    /*
     * Exposes the reference region's blended visibility — a multiplier
     * where 1.0 is clear air and lower values are hazier. Not yet wired
     * into fog rendering (that needs a WeatherData UBO/shader change —
     * see the next stage); this is the CPU-side value that stage will
     * push to the GPU. Falls back to a neutral 1.0
     * (EngineSetting.DEFAULT_WEATHER_VISIBILITY) before the first weather
     * pool ever resolves.
     */
    public float getVisibility() {

        if (!hasActiveWeatherPool())
            return EngineSetting.DEFAULT_WEATHER_VISIBILITY;

        return regionSampleBranch.getCenterSample().getVisibility();
    }

    /*
     * Exposes the reference region's live computed temperature, without
     * leaking TemperatureBranch itself.
     */
    public float getCurrentTemperature() {
        return temperatureBranch.getCurrentTemperature();
    }

    /*
     * Direct CPU-side query for the planet-rotation-driven global weather
     * noise at any world-space chunk coordinate — for systems that need more
     * than the reference-region blend already flowing through the UBOs
     * (e.g. the overhead cell system deciding its own local intensity).
     */
    public float getGlobalStormIntensityAt(long chunkCoordinate) {
        return globalNoiseBranch.sampleGlobalIntensity(chunkCoordinate);
    }

    /*
     * Resolves which weather(s) the given world-space chunk coordinate
     * currently sits between, against this manager's own active seasonal
     * pool — the same canonical noise-and-chance-band algorithm this
     * manager's own 5-point region sampling already uses. Throws if called
     * before hasActiveWeatherPool() is true — callers driving a grid (the
     * overhead cloud system) are expected to check that first and simply
     * skip work for the frame, rather than treat this as a normal path.
     */
    public void resolveWeatherBand(WeatherBandStruct out, long chunkCoordinate) {

        if (activeWeatherPool == null)
            throwException("Cannot resolve a weather band before any season has been resolved. "
                    + "Callers should check hasActiveWeatherPool() first.");

        int chunkX = Coordinate2Long.unpackX(chunkCoordinate);
        int chunkY = Coordinate2Long.unpackY(chunkCoordinate);

        regionSampleBranch.resolveBand(out, chunkX, chunkY, activeWeatherPool);
    }
}