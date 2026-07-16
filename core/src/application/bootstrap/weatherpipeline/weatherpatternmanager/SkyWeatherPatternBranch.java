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
     * Flattens every active weather pattern's cloud lobes into the
     * SkyWeatherPatternData UBO — one array slot per lobe, built from the
     * same position/size math the overhead volumetric system uses for its
     * instance boxes, so the sky dome and the physical cloud layer are
     * always the same weather. Entries are gathered and sorted far-to-near
     * before upload so the fragment shader's fixed draw-order loop
     * composites correctly with no per-pixel sorting required.
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

    // Gather / Sort Scratch
    private WeatherPatternStruct[] gatheredPatterns;
    private WeatherPatternLobeStruct[] gatheredLobes;
    private float[] gatheredDistanceSq;

    // UBO Arrays
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

        this.gatheredPatterns = new WeatherPatternStruct[MAX_CLOUDS];
        this.gatheredLobes = new WeatherPatternLobeStruct[MAX_CLOUDS];
        this.gatheredDistanceSq = new float[MAX_CLOUDS];

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

    // Push \\

    private void pushClouds() {

        long referenceCoordinate = weatherManager.getReferenceCoordinate();
        int referenceChunkX = Coordinate2Long.unpackX(referenceCoordinate);
        int referenceChunkZ = Coordinate2Long.unpackY(referenceCoordinate);

        WorldHandle activeWorld = worldManager.getActiveWorld();
        int worldWidthChunks = activeWorld.getWorldScale().x / EngineSetting.CHUNK_SIZE;
        int worldHeightChunks = activeWorld.getWorldScale().y / EngineSetting.CHUNK_SIZE;

        float cameraY = resolveCameraY();

        int count = gatherVisibleLobes(referenceChunkX, referenceChunkZ, worldWidthChunks, worldHeightChunks);
        sortGatheredFarToNear(count);

        float highestTopElevation = -(float) Math.PI * 0.5f;

        for (int index = 0; index < count; index++) {
            float topElevation = writeCloudEntry(
                    index, gatheredPatterns[index], gatheredLobes[index],
                    referenceChunkX, referenceChunkZ,
                    worldWidthChunks, worldHeightChunks,
                    cameraY);
            highestTopElevation = Math.max(highestTopElevation, topElevation);
        }

        for (int i = count; i < MAX_CLOUDS; i++)
            fadeAlpha[i] = 0f;

        float elevationLimit = Math.min(highestTopElevation + ELEVATION_LIMIT_MARGIN_RADIANS, (float) Math.PI * 0.5f);

        skyWeatherPatternData.updateUniform("u_cloudCount", count);
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

    // Gather / Sort \\

    private int gatherVisibleLobes(
            int referenceChunkX,
            int referenceChunkZ,
            int worldWidthChunks,
            int worldHeightChunks) {

        int count = 0;

        for (WeatherPatternStruct pattern : weatherPatternManager.getActivePatterns().values()) {

            if (pattern.getFadeAlpha() <= 0.001f)
                continue;

            WeatherPatternLobeStruct[] lobes = pattern.getLobes();

            for (int i = 0; i < lobes.length && count < MAX_CLOUDS; i++) {

                WeatherPatternLobeStruct lobe = lobes[i];

                if (!lobe.hasCloud())
                    continue;

                double lobeChunkX = pattern.getCurrentChunkX() + lobe.getOffsetChunkX();
                double lobeChunkZ = pattern.getCurrentChunkZ() + lobe.getOffsetChunkZ();

                double dx = WorldWrapUtility.wrappedDelta(lobeChunkX, referenceChunkX, worldWidthChunks);
                double dz = WorldWrapUtility.wrappedDelta(lobeChunkZ, referenceChunkZ, worldHeightChunks);

                gatheredPatterns[count] = pattern;
                gatheredLobes[count] = lobe;
                gatheredDistanceSq[count] = (float) (dx * dx + dz * dz);
                count++;
            }
        }

        return count;
    }

    /*
     * Insertion sort, farthest first — count never exceeds MAX_CLOUDS (64),
     * so this stays allocation-free and the O(n^2) cost is trivial.
     */
    private void sortGatheredFarToNear(int count) {

        for (int i = 1; i < count; i++) {

            WeatherPatternStruct pattern = gatheredPatterns[i];
            WeatherPatternLobeStruct lobe = gatheredLobes[i];
            float distanceSq = gatheredDistanceSq[i];

            int j = i - 1;
            while (j >= 0 && gatheredDistanceSq[j] < distanceSq) {
                gatheredPatterns[j + 1] = gatheredPatterns[j];
                gatheredLobes[j + 1] = gatheredLobes[j];
                gatheredDistanceSq[j + 1] = gatheredDistanceSq[j];
                j--;
            }

            gatheredPatterns[j + 1] = pattern;
            gatheredLobes[j + 1] = lobe;
            gatheredDistanceSq[j + 1] = distanceSq;
        }
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