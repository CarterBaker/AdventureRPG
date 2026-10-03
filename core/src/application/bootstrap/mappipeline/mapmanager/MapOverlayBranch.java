package application.bootstrap.mappipeline.mapmanager;

import java.nio.ByteBuffer;

import application.bootstrap.calendarpipeline.clockmanager.ClockManager;
import application.bootstrap.calendarpipeline.util.CelestialUtility;
import application.bootstrap.mappipeline.util.MapShadeUtility;
import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import application.bootstrap.weatherpipeline.util.SkyColorUtility;
import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import application.bootstrap.weatherpipeline.weathermanager.WeatherManager;
import application.bootstrap.weatherpipeline.weatherpatternmanager.WeatherPatternManager;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.graphics.color.PackedColorUtility;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.memory.BufferUtility;

class MapOverlayBranch extends BranchPackage {

    /*
     * Main thread — keeps the two overlays a map view can show over its tiles,
     * both read from the shared world state every grid follows, so every view,
     * Dev window and preview agrees. Day and night is one texel per band of
     * the world's north-south span, each the darkness of the visual time of
     * day ClockManager gives that band, rebuilt every frame it is shown. The
     * weather covers the whole world in noise space, each texel the cloud
     * cover and precipitation of the weather cell beneath it, resampled a
     * budget of texels per frame and uploaded once a pass completes; views
     * slide it by the weather flow, so clouds drift smoothly between passes.
     * Each overlay only does work while some view showed it this frame or
     * the last.
     */

    // Internal
    private TextureManager textureManager;
    private ClockManager clockManager;
    private WeatherManager weatherManager;
    private WeatherPatternManager weatherPatternManager;

    // World
    private WorldHandle worldHandle;

    // Day and Night
    private int daylightTexture;
    private byte[] daylightPixels;
    private ByteBuffer daylightBuffer;
    private long dayNightRequestFrame;
    private boolean daylightReady;

    // Weather
    private int weatherTexture;
    private int weatherTexelsX;
    private int weatherTexelsZ;
    private byte[] weatherPixels;
    private ByteBuffer weatherBuffer;
    private int weatherCursor;
    private long weatherRequestFrame;
    private boolean weatherReady;

    // Base \\

    @Override
    protected void create() {

        // Day and Night
        this.daylightPixels = new byte[EngineSetting.MAP_OVERLAY_DAYLIGHT_ROWS * EngineSetting.COLOR_CHANNEL_COUNT];
        this.daylightBuffer = BufferUtility.newByteBuffer(daylightPixels.length);
        this.dayNightRequestFrame = EngineSetting.INDEX_NOT_FOUND;

        // Weather
        this.weatherRequestFrame = EngineSetting.INDEX_NOT_FOUND;
    }

    @Override
    protected void get() {
        this.textureManager = get(TextureManager.class);
        this.clockManager = get(ClockManager.class);
        this.weatherManager = get(WeatherManager.class);
        this.weatherPatternManager = get(WeatherPatternManager.class);
    }

    @Override
    protected void dispose() {

        if (daylightTexture != 0)
            textureManager.deleteTexture2D(daylightTexture);

        if (weatherTexture != 0)
            textureManager.deleteTexture2D(weatherTexture);
    }

    // Request \\

    void requestDayNight(long frame) {
        this.dayNightRequestFrame = frame;
    }

    void requestWeather(long frame) {
        this.weatherRequestFrame = frame;
    }

    // Update \\

    void update(WorldHandle nextWorldHandle, long frame) {

        resolveWorld(nextWorldHandle);

        if (dayNightRequestFrame >= frame - 1)
            refreshDaylight();

        if (weatherRequestFrame >= frame - 1)
            refreshWeather();
    }

    // World \\

    // The weather texture keeps the world's aspect, so its texels stay square on the map
    private void resolveWorld(WorldHandle nextWorldHandle) {

        if (nextWorldHandle == worldHandle)
            return;

        this.worldHandle = nextWorldHandle;
        this.daylightReady = false;
        this.weatherReady = false;
        this.weatherCursor = 0;

        int texelsX = EngineSetting.MAP_OVERLAY_WEATHER_TEXELS;
        int texelsZ = Math.max(1, (int) Math.round(
                texelsX * (double) nextWorldHandle.getWorldScale().y / nextWorldHandle.getWorldScale().x));

        if (texelsX == weatherTexelsX && texelsZ == weatherTexelsZ)
            return;

        if (weatherTexture != 0)
            textureManager.deleteTexture2D(weatherTexture);

        this.weatherTexelsX = texelsX;
        this.weatherTexelsZ = texelsZ;
        this.weatherPixels = new byte[texelsX * texelsZ * EngineSetting.COLOR_CHANNEL_COUNT];
        this.weatherBuffer = BufferUtility.newByteBuffer(weatherPixels.length);
        this.weatherTexture = textureManager.createTexture2D(
                texelsX, texelsZ, EngineSetting.GL_REPEAT, EngineSetting.GL_LINEAR);
    }

    // Day and Night \\

    private void refreshDaylight() {

        if (daylightTexture == 0)
            this.daylightTexture = textureManager.createTexture2D(
                    1, EngineSetting.MAP_OVERLAY_DAYLIGHT_ROWS, EngineSetting.GL_REPEAT, EngineSetting.GL_LINEAR);

        int rows = EngineSetting.MAP_OVERLAY_DAYLIGHT_ROWS;
        int worldHeightChunks = worldHandle.getWorldScale().y / EngineSetting.CHUNK_SIZE;

        for (int row = 0; row < rows; row++) {

            int chunkZ = (int) ((row + 0.5) / rows * worldHeightChunks);
            double visualTimeOfDay = clockManager.computeVisualTimeOfDay(worldHandle, Coordinate2Long.pack(0, chunkZ));
            float night = 1f - SkyColorUtility.smoothstep(
                    EngineSetting.MAP_NIGHT_ELEVATION_DARK,
                    EngineSetting.MAP_NIGHT_ELEVATION_LIGHT,
                    (float) CelestialUtility.resolveSolarElevation(visualTimeOfDay));

            MapShadeUtility.writeTexel(daylightPixels, row, PackedColorUtility.packUnit(night, 0f, 0f));
        }

        daylightBuffer.clear();
        daylightBuffer.put(daylightPixels);
        textureManager.updateTexture2D(daylightTexture, 0, 0, 1, rows, daylightBuffer);

        this.daylightReady = true;
    }

    // Weather \\

    private void refreshWeather() {

        if (!weatherManager.hasActiveWeatherPool() || weatherPatternManager.getMapWorld() != worldHandle)
            return;

        int texelCount = weatherTexelsX * weatherTexelsZ;
        double texelBlocksX = worldHandle.getWorldScale().x / (double) weatherTexelsX;
        double texelBlocksZ = worldHandle.getWorldScale().y / (double) weatherTexelsZ;

        for (int i = 0; i < EngineSetting.MAP_OVERLAY_WEATHER_SAMPLES_PER_FRAME; i++) {

            int x = weatherCursor % weatherTexelsX;
            int z = weatherCursor / weatherTexelsX;
            WeatherHandle weather = weatherPatternManager.sampleNoiseWeather(
                    (x + 0.5) * texelBlocksX,
                    (z + 0.5) * texelBlocksZ);

            MapShadeUtility.writeTexel(weatherPixels, weatherCursor, PackedColorUtility.packUnit(
                    weather.getCloudCoverage(),
                    weather.getPrecipitationIntensity(),
                    0f));

            weatherCursor++;

            if (weatherCursor == texelCount) {
                uploadWeather();
                weatherCursor = 0;
                return;
            }
        }
    }

    private void uploadWeather() {

        weatherBuffer.clear();
        weatherBuffer.put(weatherPixels);
        textureManager.updateTexture2D(weatherTexture, 0, 0, weatherTexelsX, weatherTexelsZ, weatherBuffer);

        this.weatherReady = true;
    }

    // Accessible \\

    boolean isDaylightReady() {
        return daylightReady;
    }

    boolean isWeatherReady() {
        return weatherReady;
    }

    int getDaylightTexture() {
        return daylightTexture;
    }

    int getWeatherTexture() {
        return weatherTexture;
    }

    // Noise space lags the world by the flow, so a view reads the weather this far behind its own world position
    float getWeatherOffsetU() {
        return (float) CelestialUtility.wrapFraction(
                weatherManager.getFlowOffsetXBlocks() / worldHandle.getWorldScale().x);
    }

    float getWeatherOffsetV() {
        return (float) CelestialUtility.wrapFraction(
                weatherManager.getFlowOffsetZBlocks() / worldHandle.getWorldScale().y);
    }
}
