package application.bootstrap.weatherpipeline.weathermanager;

import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import application.bootstrap.worldpipeline.biome.BiomeHandle;
import application.bootstrap.worldpipeline.biome.WeatherChanceStruct;
import application.bootstrap.worldpipeline.biomemanager.BiomeManager;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.extras.Coordinate2Long;
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
     * activeWeatherPool stays null until the calendar resolves its first
     * named season, which requires at least one day-tick — normal at
     * startup. update() skips sampling entirely until then, and any
     * cross-package caller of resolveWeatherBand() should check
     * hasActiveWeatherPool() first; calling it before that point throws
     * rather than silently resolving against nothing.
     */

    // Internal
    private ClockManager clockManager;
    private BiomeManager biomeManager;

    // Branches
    private GlobalNoiseBranch globalNoiseBranch;
    private RegionSampleBranch regionSampleBranch;
    private TemperatureBranch temperatureBranch;
    private InternalBufferBranch internalBuffer;

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
        this.internalBuffer = create(InternalBufferBranch.class);

        create(InternalLoader.class);
    }

    @Override
    protected void get() {

        // Internal
        this.clockManager = get(ClockManager.class);
        this.biomeManager = get(BiomeManager.class);
    }

    @Override
    protected void awake() {
        this.internalBuffer.assignData(regionSampleBranch);
    }

    @Override
    protected void update() {

        String currentSeason = clockManager.getClockHandle().getCurrentSeason();

        if (currentSeason != null && !currentSeason.equals(lastSeason)) {
            BiomeHandle activeBiome = biomeManager.getBiomeHandleFromBiomeName(EngineSetting.DEFAULT_BIOME_NAME);
            this.activeWeatherPool = resolveWeatherPool(activeBiome, currentSeason);
            this.lastSeason = currentSeason;
        }

        if (activeWeatherPool == null)
            return;

        regionSampleBranch.sampleRegions(activeWeatherPool);
        temperatureBranch.updateTemperature(regionSampleBranch.getCenterSample());
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
        ((InternalLoader) internalLoader).request(weatherName);
    }

    // Biome Resolution \\

    /*
     * Resolves a biome's named-season weather chance entries into live
     * handles, preserving JSON declaration order — RegionSampleBranch blends
     * across this pool by chance-weighted band, not array position, so no
     * sort is needed here.
     */
    private ObjectArrayList<WeatherPoolEntryStruct> resolveWeatherPool(BiomeHandle biomeHandle, String season) {

        ObjectArrayList<WeatherChanceStruct> entries = biomeHandle.getWeatherEntriesForSeason(season);

        if (entries.isEmpty())
            throwException("Biome \"" + biomeHandle.getBiomeName() +
                    "\" has no weathers defined for season \"" + season + "\"");

        ObjectArrayList<WeatherPoolEntryStruct> pool = new ObjectArrayList<>(entries.size());

        for (int i = 0; i < entries.size(); i++) {
            WeatherChanceStruct entry = entries.get(i);
            WeatherHandle handle = getWeatherHandleFromWeatherName(entry.getWeatherName());
            pool.add(new WeatherPoolEntryStruct(handle, entry.getChance()));
        }

        return pool;
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
     */
    public float getWindSpeedScale() {
        return regionSampleBranch.getCenterSample().getWindSpeedScale();
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