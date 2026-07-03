package application.bootstrap.weatherpipeline.weathermanager;

import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.weatherpipeline.season.Season;
import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import application.bootstrap.worldpipeline.biome.BiomeHandle;
import application.bootstrap.worldpipeline.biome.WeatherChanceStruct;
import application.bootstrap.worldpipeline.biomemanager.BiomeManager;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2ShortOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;

public class WeatherManager extends ManagerPackage {

    /*
     * Owns the weather palette for the engine lifetime and drives the live
     * weather simulation. Supports on-demand loading via InternalLoader on a
     * cache miss, keyed by weather name (e.g. "standard/Sunny"). The active
     * seasonal pool is re-resolved only when the season changes; per-frame
     * region sampling and the GPU UBO pushes are delegated to branches.
     */

    // Internal
    private ClockManager clockManager;
    private BiomeManager biomeManager;

    // Branches
    private RegionSampleBranch regionSampleBranch;
    private InternalBufferBranch internalBuffer;

    // Palette
    private Object2ShortOpenHashMap<String> weatherName2WeatherID;
    private Short2ObjectOpenHashMap<WeatherHandle> weatherID2WeatherHandle;

    // Season Tracking
    private Season lastSeason;
    private ObjectArrayList<WeatherPoolEntryStruct> activeWeatherPool;

    // Base \\

    @Override
    protected void create() {

        // Palette
        this.weatherName2WeatherID = new Object2ShortOpenHashMap<>();
        this.weatherID2WeatherHandle = new Short2ObjectOpenHashMap<>();

        // Branches
        this.regionSampleBranch = create(RegionSampleBranch.class);
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

        Season currentSeason = clockManager.getClockHandle().getCurrentSeason();

        if (currentSeason != lastSeason) {
            BiomeHandle activeBiome = biomeManager.getBiomeHandleFromBiomeName(EngineSetting.DEFAULT_BIOME_NAME);
            this.activeWeatherPool = resolveWeatherPool(activeBiome, currentSeason);
            this.lastSeason = currentSeason;
        }

        regionSampleBranch.sampleRegions(activeWeatherPool);
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
     * Resolves a biome's seasonal weather chance entries into live handles,
     * preserving JSON declaration order — RegionSampleBranch blends across
     * this pool by chance-weighted band, not array position, so no sort
     * is needed here.
     */
    private ObjectArrayList<WeatherPoolEntryStruct> resolveWeatherPool(BiomeHandle biomeHandle, Season season) {

        ObjectArrayList<WeatherChanceStruct> entries = biomeHandle.getWeatherEntriesForSeason(season);

        if (entries.isEmpty())
            throwException("Biome \"" + biomeHandle.getBiomeName() +
                    "\" has no weathers defined for season " + season);

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

    /*
     * Exposes the reference region's blended windSpeedScale for WindManager's
     * LocalWindBranch, without leaking the package-private WeatherSampleStruct.
     */
    public float getWindSpeedScale() {
        return regionSampleBranch.getCenterSample().getWindSpeedScale();
    }
}