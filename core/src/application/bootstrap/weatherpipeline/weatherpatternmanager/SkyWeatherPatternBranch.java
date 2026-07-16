package application.bootstrap.weatherpipeline.weatherpatternmanager;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import application.bootstrap.weatherpipeline.cloud.CloudHandle;
import application.bootstrap.weatherpipeline.weathermanager.WeatherManager;
import application.bootstrap.worldpipeline.util.WorldWrapUtility;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import application.kernel.windowpipeline.window.WindowInstance;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate2Long;
import engine.util.mathematics.vectors.Vector3;

class SkyWeatherPatternBranch extends BranchPackage {

    /*
     * Flattens every active weather pattern's individual cloud lobes into the
     * SkyWeatherPatternData UBO — one array slot per real cloud lobe, built
     * from the exact same position/size math the overhead volumetric system
     * uses for its instance boxes (see CloudVolumeShader.vsh), so the sky
     * dome preview and the physical cloud layer are the same weather rather
     * than two independently-approximated shapes. Lobes with no cloud
     * (clear/sunny weather) contribute nothing — clear sky stays clear.
     * MAX_CLOUDS must stay equal to SKY_CLOUD_MAX_COUNT in the matching
     * SkyWeatherPatternData.glsl.
     */

    private static final int MAX_CLOUDS = EngineSetting.WEATHER_PATTERN_OVERHEAD_LOBE_BUDGET;
    private static final float ELEVATION_LIMIT_MARGIN_RADIANS = (float) Math.toRadians(8.0);

    private WeatherPatternManager weatherPatternManager;
    private WeatherManager weatherManager;
    private WorldManager worldManager;
    private UBOManager uboManager;
    private PlayerManager playerManager;
    private WindowManager windowManager;

    private UBOHandle skyWeatherPatternData;

    private Vector3[] center;
    private Vector3[] halfExtent;
    private Float[] domainRotation;
    private Float[] fadeAlpha;
    private Float[] intensity;
    private Vector3[] color;
    private Vector3[] topColor;
    private Vector3[] shadowColor;
    private Float[] density;
    private Float[] shadeStrength;
    private Float[] rimLightStrength;
    private Float[] ambientOcclusionStrength;
    private Float[] brightnessMultiplier;
    private Float[] toonBands;
    private Float[] densityNoiseScale;
    private Float[] noiseWarpStrength;
    private Float[] coverageBias;
    private Float[] silhouetteSoftness;
    private Float[] seed;

    @Override
    protected void create() {
        this.center = newVector3Array();
        this.halfExtent = newVector3Array();
        this.domainRotation = newFloatArray();
        this.fadeAlpha = newFloatArray();
        this.intensity = newFloatArray();
        this.color = newVector3Array();
        this.topColor = newVector3Array();
        this.shadowColor = newVector3Array();
        this.density = newFloatArray();
        this.shadeStrength = newFloatArray();
        this.rimLightStrength = newFloatArray();
        this.ambientOcclusionStrength = newFloatArray();
        this.brightnessMultiplier = newFloatArray();
        this.toonBands = newFloatArray();
        this.densityNoiseScale = newFloatArray();
        this.noiseWarpStrength = newFloatArray();
        this.coverageBias = newFloatArray();
        this.silhouetteSoftness = newFloatArray();
        this.seed = newFloatArray();
    }

    @Override
    protected void get() {
        this.weatherPatternManager = get(WeatherPatternManager.class);
        this.weatherManager = get(WeatherManager.class);
        this.worldManager = get(WorldManager.class);
        this.uboManager = get(UBOManager.class);
        this.playerManager = get(PlayerManager.class);
        this.windowManager = get(WindowManager.class);
    }

    @Override
    protected void awake() {
        this.skyWeatherPatternData = uboManager.getUBOHandleFromUBOName(EngineSetting.SKY_WEATHER_PATTERN_DATA_UBO);
    }

    @Override
    protected void update() {
        pushClouds();
    }

    private void pushClouds() {

        long referenceCoordinate = weatherManager.getReferenceCoordinate();
        int referenceChunkX = Coordinate2Long.unpackX(referenceCoordinate);
        int referenceChunkZ = Coordinate2Long.unpackY(referenceCoordinate);

        WorldHandle activeWorld = worldManager.getActiveWorld();
        int worldWidthChunks = activeWorld.getWorldScale().x / EngineSetting.CHUNK_SIZE;
        int worldHeightChunks = activeWorld.getWorldScale().y / EngineSetting.CHUNK_SIZE;

        float cameraY = resolveCameraY();

        int index = 0;
        float highestTopElevation = -(float) Math.PI * 0.5f;

        for (WeatherPatternStruct pattern : weatherPatternManager.getActivePatterns().values()) {

            if (pattern.getFadeAlpha() <= 0.001f)
                continue;

            WeatherPatternLobeStruct[] lobes = pattern.getLobes();

            for (int i = 0; i < lobes.length && index < MAX_CLOUDS; i++) {

                WeatherPatternLobeStruct lobe = lobes[i];

                if (!lobe.hasCloud())
                    continue;

                float topElevation = writeCloudEntry(
                        index, pattern, lobe,
                        referenceChunkX, referenceChunkZ,
                        worldWidthChunks, worldHeightChunks,
                        cameraY);

                highestTopElevation = Math.max(highestTopElevation, topElevation);
                index++;
            }
        }

        for (int i = index; i < MAX_CLOUDS; i++)
            fadeAlpha[i] = 0f;

        float elevationLimit = Math.min(highestTopElevation + ELEVATION_LIMIT_MARGIN_RADIANS, (float) Math.PI * 0.5f);

        skyWeatherPatternData.updateUniform("u_cloudCount", index);
        skyWeatherPatternData.updateUniform("u_cloudCenter", center);
        skyWeatherPatternData.updateUniform("u_cloudHalfExtent", halfExtent);
        skyWeatherPatternData.updateUniform("u_cloudDomainRotation", domainRotation);
        skyWeatherPatternData.updateUniform("u_cloudFadeAlpha", fadeAlpha);
        skyWeatherPatternData.updateUniform("u_cloudIntensity", intensity);
        skyWeatherPatternData.updateUniform("u_cloudColor", color);
        skyWeatherPatternData.updateUniform("u_cloudTopColor", topColor);
        skyWeatherPatternData.updateUniform("u_cloudShadowColor", shadowColor);
        skyWeatherPatternData.updateUniform("u_cloudDensity", density);
        skyWeatherPatternData.updateUniform("u_cloudShadeStrength", shadeStrength);
        skyWeatherPatternData.updateUniform("u_cloudRimLightStrength", rimLightStrength);
        skyWeatherPatternData.updateUniform("u_cloudAmbientOcclusionStrength", ambientOcclusionStrength);
        skyWeatherPatternData.updateUniform("u_cloudBrightnessMultiplier", brightnessMultiplier);
        skyWeatherPatternData.updateUniform("u_cloudToonBands", toonBands);
        skyWeatherPatternData.updateUniform("u_cloudDensityNoiseScale", densityNoiseScale);
        skyWeatherPatternData.updateUniform("u_cloudNoiseWarpStrength", noiseWarpStrength);
        skyWeatherPatternData.updateUniform("u_cloudCoverageBias", coverageBias);
        skyWeatherPatternData.updateUniform("u_cloudSilhouetteSoftness", silhouetteSoftness);
        skyWeatherPatternData.updateUniform("u_cloudSeed", seed);
        skyWeatherPatternData.updateUniform("u_skyElevationLimit", (float) Math.sin(elevationLimit));

        uboManager.push(skyWeatherPatternData);
    }

    private float writeCloudEntry(
            int index,
            WeatherPatternStruct pattern,
            WeatherPatternLobeStruct lobe,
            int referenceChunkX,
            int referenceChunkZ,
            int worldWidthChunks,
            int worldHeightChunks,
            float cameraY) {

        CloudHandle cloud = lobe.getCloudHandle();

        double lobeChunkX = pattern.getCurrentChunkX() + lobe.getOffsetChunkX();
        double lobeChunkZ = pattern.getCurrentChunkZ() + lobe.getOffsetChunkZ();

        double dx = WorldWrapUtility.wrappedDelta(lobeChunkX, referenceChunkX, worldWidthChunks);
        double dz = WorldWrapUtility.wrappedDelta(lobeChunkZ, referenceChunkZ, worldHeightChunks);

        float sizeVariance = lobe.getSizeVariance();
        float elongation = Math.max(lobe.getElongation(), 1f);

        float halfX = (cloud.getScale() * sizeVariance * elongation) * 0.5f;
        float halfZ = (cloud.getScale() * sizeVariance) * 0.5f;
        float verticalThickness = cloud.getVerticalThickness() * sizeVariance;
        float halfY = verticalThickness * 0.5f;

        float baseAltitude = lobe.getEffectiveAltitude();

        float centerX = (float) (dx * EngineSetting.CHUNK_SIZE);
        float centerZ = (float) (dz * EngineSetting.CHUNK_SIZE);
        float centerY = baseAltitude + halfY;

        center[index].set(centerX, centerY, centerZ);
        halfExtent[index].set(halfX, halfY, halfZ);
        domainRotation[index] = lobe.getDomainRotation();
        fadeAlpha[index] = pattern.getFadeAlpha();
        intensity[index] = pattern.getIntensity();

        color[index].set(cloud.getCloudColor());
        topColor[index].set(cloud.getTopColor());
        shadowColor[index].set(cloud.getShadowColor());
        density[index] = cloud.getDensity();
        shadeStrength[index] = cloud.getShadeStrength();
        rimLightStrength[index] = cloud.getRimLightStrength();
        ambientOcclusionStrength[index] = cloud.getAmbientOcclusionStrength();
        brightnessMultiplier[index] = cloud.getBrightnessMultiplier();
        toonBands[index] = (float) cloud.getToonBands();
        densityNoiseScale[index] = cloud.getDensityNoiseScale();
        noiseWarpStrength[index] = cloud.getNoiseWarpStrength();
        coverageBias[index] = cloud.getCoverageBias();
        silhouetteSoftness[index] = cloud.getSilhouetteSoftness();
        seed[index] = lobe.getRandomSeed();

        float distanceBlocks = Math.max((float) Math.sqrt(centerX * centerX + centerZ * centerZ), 1f);
        float topRelativeY = (baseAltitude + verticalThickness) - cameraY;

        return (float) Math.atan2(topRelativeY, distanceBlocks);
    }

    private Float[] newFloatArray() {
        Float[] array = new Float[MAX_CLOUDS];
        java.util.Arrays.fill(array, 0f);
        return array;
    }

    private Vector3[] newVector3Array() {
        Vector3[] array = new Vector3[MAX_CLOUDS];
        for (int i = 0; i < MAX_CLOUDS; i++)
            array[i] = new Vector3();
        return array;
    }

    private float resolveCameraY() {

        WindowInstance mainWindow = windowManager.getMainWindow();

        if (mainWindow == null)
            return 0f;

        int windowID = mainWindow.getWindowID();

        if (!playerManager.hasPlayerForWindow(windowID))
            return 0f;

        EntityInstance player = playerManager.getPlayerForWindow(windowID);

        return player.getWorldPositionStruct().getPosition().y;
    }
}