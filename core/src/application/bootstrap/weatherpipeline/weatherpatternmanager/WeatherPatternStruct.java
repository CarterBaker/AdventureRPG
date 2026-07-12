// WeatherPatternStruct.java
package application.bootstrap.weatherpipeline.weatherpatternmanager;

import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import engine.root.StructPackage;

public class WeatherPatternStruct extends StructPackage {

    /*
     * One persistent, large-scale weather system shared by both the overhead
     * volumetric layer and the sky dome. Identity, lobe layout, and UBO slot
     * are fixed for its lifetime; only drift, fade, and intensity change.
     */

    private final long patternKey;
    private final WeatherHandle weatherHandle;

    private final int homeChunkX;
    private final int homeChunkZ;

    private final WeatherPatternLobeStruct[] lobes;
    private final float driftSpeedScale;

    // Stable UBO slot assigned by WeatherPatternManager's free-slot pool at
    // stream-in and released back to it at retirement — never reassigned
    // for the life of this pattern, so SkyWeatherPatternBranch always writes
    // this pattern's data to the same array index every frame.
    private final int slot;

    private double driftChunkX;
    private double driftChunkZ;

    private float fadeAlpha;
    private boolean retiring;

    private float intensity;

    private double nextReevaluationTime;

    public WeatherPatternStruct(
            long patternKey,
            int homeChunkX,
            int homeChunkZ,
            WeatherHandle weatherHandle,
            WeatherPatternLobeStruct[] lobes,
            float driftSpeedScale,
            float intensity,
            int slot) {

        this.patternKey = patternKey;
        this.weatherHandle = weatherHandle;
        this.homeChunkX = homeChunkX;
        this.homeChunkZ = homeChunkZ;
        this.lobes = lobes;
        this.driftSpeedScale = driftSpeedScale;
        this.slot = slot;
        this.fadeAlpha = 0f;
        this.retiring = false;
        this.intensity = intensity;
    }

    public void advanceDrift(double deltaChunkX, double deltaChunkZ) {
        this.driftChunkX += deltaChunkX;
        this.driftChunkZ += deltaChunkZ;
    }

    public void setRetiring(boolean retiring) {
        this.retiring = retiring;
    }

    public void setFadeAlpha(float fadeAlpha) {
        this.fadeAlpha = fadeAlpha;
    }

    public void setIntensity(float intensity) {
        this.intensity = intensity;
    }

    public double getNextReevaluationTime() {
        return nextReevaluationTime;
    }

    public void setNextReevaluationTime(double nextReevaluationTime) {
        this.nextReevaluationTime = nextReevaluationTime;
    }

    public long getPatternKey() {
        return patternKey;
    }

    public WeatherHandle getWeatherHandle() {
        return weatherHandle;
    }

    public int getHomeChunkX() {
        return homeChunkX;
    }

    public int getHomeChunkZ() {
        return homeChunkZ;
    }

    public WeatherPatternLobeStruct[] getLobes() {
        return lobes;
    }

    public int getLobeCount() {
        return lobes.length;
    }

    public float getDriftSpeedScale() {
        return driftSpeedScale;
    }

    public int getSlot() {
        return slot;
    }

    public double getCurrentChunkX() {
        return homeChunkX + driftChunkX;
    }

    public double getCurrentChunkZ() {
        return homeChunkZ + driftChunkZ;
    }

    public float getFadeAlpha() {
        return fadeAlpha;
    }

    public boolean isRetiring() {
        return retiring;
    }

    public float getIntensity() {
        return intensity;
    }
}