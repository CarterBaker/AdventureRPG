package application.bootstrap.weatherpipeline.weather;

import engine.root.EngineSetting;
import engine.root.InstancePackage;
import engine.util.mathematics.vectors.Vector4;

public class WeatherInstance extends InstancePackage {

    /*
     * One weather occurrence — either a pool-recycled spatial cell streamed
     * into the shared weather map, or the single instance a grid holds for
     * its own location's wind, temperature, and humidity. Every visual and
     * atmospheric value blends between previousWeatherHandle and
     * weatherHandle across transitionT, so a weather change cross-fades
     * instead of cutting. Position only ever changes via advancePosition(),
     * independent of weather reevaluation.
     */

    // Identity
    private int slot;
    private boolean configured;

    // Pattern
    private long patternKey;

    private WeatherHandle weatherHandle;
    private WeatherHandle previousWeatherHandle;
    private float transitionT;

    private int homeChunkX;
    private int homeChunkZ;

    private float driftSpeedScale;

    // Position — the only mutation path is advancePosition(), a continuous
    // integration of the persisted velocity below.
    private double positionX;
    private double positionZ;
    private double velocityXChunksPerSecond;
    private double velocityZChunksPerSecond;

    private float fadeAlpha;
    private boolean retiring;

    private double nextReevaluationTime;

    private float distanceFromReferenceChunks;
    private Vector4 bounds;

    // Internal \\

    @Override
    protected void create() {
        this.bounds = new Vector4();
        this.slot = -1;
        this.configured = false;
    }

    // Identity \\

    public void assignSlot(int slot) {
        this.slot = slot;
    }

    public int getSlot() {
        return slot;
    }

    public boolean isConfigured() {
        return configured;
    }

    // Constructor \\

    public void constructor(
            long patternKey,
            int homeChunkX,
            int homeChunkZ,
            WeatherHandle weatherHandle,
            float driftSpeedScale) {

        this.patternKey = patternKey;
        this.weatherHandle = weatherHandle;
        this.previousWeatherHandle = weatherHandle;
        this.transitionT = 1f;
        this.homeChunkX = homeChunkX;
        this.homeChunkZ = homeChunkZ;
        this.driftSpeedScale = driftSpeedScale;
        this.positionX = homeChunkX;
        this.positionZ = homeChunkZ;
        this.velocityXChunksPerSecond = 0.0;
        this.velocityZChunksPerSecond = 0.0;
        this.fadeAlpha = 0f;
        this.retiring = false;
        this.distanceFromReferenceChunks = 0f;
        this.configured = true;
    }

    // Velocity \\

    public void setVelocity(double velocityXChunksPerSecond, double velocityZChunksPerSecond) {
        this.velocityXChunksPerSecond = velocityXChunksPerSecond;
        this.velocityZChunksPerSecond = velocityZChunksPerSecond;
    }

    public double getVelocityXChunksPerSecond() {
        return velocityXChunksPerSecond;
    }

    public double getVelocityZChunksPerSecond() {
        return velocityZChunksPerSecond;
    }

    public void setDriftSpeedScale(float driftSpeedScale) {
        this.driftSpeedScale = driftSpeedScale;
    }

    public float getDriftSpeedScale() {
        return driftSpeedScale;
    }

    // Position \\

    public void advancePosition(double deltaTime) {
        positionX += velocityXChunksPerSecond * deltaTime;
        positionZ += velocityZChunksPerSecond * deltaTime;
    }

    public double getCurrentChunkX() {
        return positionX;
    }

    public double getCurrentChunkZ() {
        return positionZ;
    }

    // Weather Transition \\

    public void beginWeatherTransition(WeatherHandle newWeatherHandle) {
        this.previousWeatherHandle = this.weatherHandle;
        this.weatherHandle = newWeatherHandle;
        this.transitionT = 0f;
    }

    public void advanceWeatherTransition(float deltaTime) {
        if (transitionT >= 1f)
            return;
        transitionT = Math.min(1f, transitionT + deltaTime / EngineSetting.WEATHER_PATTERN_TRANSITION_DURATION_SECONDS);
    }

    public float getTransitionT() {
        return transitionT;
    }

    public float getEasedTransitionT() {
        if (transitionT >= 1f)
            return 1f;
        float t = Math.max(0f, transitionT);
        return t * t * (3f - 2f * t);
    }

    public WeatherHandle getPreviousWeatherHandle() {
        return previousWeatherHandle;
    }

    public WeatherHandle getWeatherHandle() {
        return weatherHandle;
    }

    // Lifecycle \\

    public void setRetiring(boolean retiring) {
        this.retiring = retiring;
    }

    public boolean isRetiring() {
        return retiring;
    }

    public void setFadeAlpha(float fadeAlpha) {
        this.fadeAlpha = fadeAlpha;
    }

    public float getFadeAlpha() {
        return fadeAlpha;
    }

    // Reevaluation \\

    public double getNextReevaluationTime() {
        return nextReevaluationTime;
    }

    public void setNextReevaluationTime(double nextReevaluationTime) {
        this.nextReevaluationTime = nextReevaluationTime;
    }

    public long getPatternKey() {
        return patternKey;
    }

    public int getHomeChunkX() {
        return homeChunkX;
    }

    public int getHomeChunkZ() {
        return homeChunkZ;
    }

    // Blended Atmosphere \\

    public float getBlendedTemperatureModifier() {
        return lerp(previousWeatherHandle.getTemperatureModifier(), weatherHandle.getTemperatureModifier(),
                getEasedTransitionT());
    }

    public float getBlendedPrecipitationIntensity() {
        return lerp(previousWeatherHandle.getPrecipitationIntensity(), weatherHandle.getPrecipitationIntensity(),
                getEasedTransitionT());
    }

    public float getBlendedWindSpeedScale() {
        return lerp(previousWeatherHandle.getWindSpeedScale(), weatherHandle.getWindSpeedScale(),
                getEasedTransitionT());
    }

    public float getBlendedWindTurbulenceScale() {
        return lerp(previousWeatherHandle.getWindTurbulenceScale(), weatherHandle.getWindTurbulenceScale(),
                getEasedTransitionT());
    }

    public float getBlendedHumidity() {
        return lerp(previousWeatherHandle.getHumidity(), weatherHandle.getHumidity(), getEasedTransitionT());
    }

    public float getBlendedVisibility() {
        return lerp(previousWeatherHandle.getVisibility(), weatherHandle.getVisibility(), getEasedTransitionT());
    }

    public float getBlendedFogDensityScale() {
        return lerp(previousWeatherHandle.getFogDensityScale(), weatherHandle.getFogDensityScale(),
                getEasedTransitionT());
    }

    public float getBlendedCloudCoverage() {
        return lerp(previousWeatherHandle.getCloudCoverage(), weatherHandle.getCloudCoverage(), getEasedTransitionT());
    }

    public float getBlendedCloudDensityMultiplier() {
        return lerp(previousWeatherHandle.getCloudDensityMultiplier(), weatherHandle.getCloudDensityMultiplier(),
                getEasedTransitionT());
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    // Spatial Data \\

    public void setDistanceFromReferenceChunks(float distanceFromReferenceChunks) {
        this.distanceFromReferenceChunks = distanceFromReferenceChunks;
    }

    public float getDistanceFromReferenceChunks() {
        return distanceFromReferenceChunks;
    }

    public void updateBounds() {

        float radius = getFootprintRadiusChunks();
        double currentX = getCurrentChunkX();
        double currentZ = getCurrentChunkZ();

        bounds.set(
                (float) (currentX - radius),
                (float) (currentZ - radius),
                (float) (currentX + radius),
                (float) (currentZ + radius));
    }

    public Vector4 getBounds() {
        return bounds;
    }

    public float getFootprintRadiusChunks() {
        float blendedVisualScale = lerp(
                previousWeatherHandle.getVisualScale(), weatherHandle.getVisualScale(), getEasedTransitionT());
        return (EngineSetting.WEATHER_PATTERN_SKY_FOOTPRINT_CHUNKS * 0.5f) * blendedVisualScale;
    }
}