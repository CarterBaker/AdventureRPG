package application.bootstrap.weatherpipeline.overheadmanager;

import application.bootstrap.weatherpipeline.cloud.CloudHandle;
import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import application.bootstrap.weatherpipeline.weatherpatternmanager.WeatherPatternLobeStruct;
import application.bootstrap.weatherpipeline.weatherpatternmanager.WeatherPatternStruct;
import engine.root.StructPackage;

public class OverheadCellStruct extends StructPackage {

    /*
     * One renderable cloud-volume placement — what CloudRenderSystem turns
     * into a single ModelInstance. A thin read-through view of one lobe
     * belonging to one WeatherPatternStruct; every getter delegates to the
     * owning pattern and/or lobe, so position, fade, and intensity always
     * track whatever the pattern most recently resolved to.
     */

    private final long cellKey;
    private final WeatherPatternStruct pattern;
    private final WeatherPatternLobeStruct lobe;

    OverheadCellStruct(long cellKey, WeatherPatternStruct pattern, WeatherPatternLobeStruct lobe) {
        this.cellKey = cellKey;
        this.pattern = pattern;
        this.lobe = lobe;
    }

    public long getCellKey() {
        return cellKey;
    }

    public WeatherHandle getWeatherHandle() {
        return pattern.getWeatherHandle();
    }

    public CloudHandle getCloudHandle() {
        return lobe.getCloudHandle();
    }

    public boolean hasCloud() {
        return lobe.hasCloud();
    }

    public float getEffectiveAltitude() {
        return lobe.getEffectiveAltitude();
    }

    public float getRandomSeed() {
        return lobe.getRandomSeed();
    }

    public float getSizeVariance() {
        return lobe.getSizeVariance();
    }

    public float getDomainRotation() {
        return lobe.getDomainRotation();
    }

    public double getCurrentChunkX() {
        return pattern.getCurrentChunkX() + lobe.getOffsetChunkX();
    }

    public double getCurrentChunkZ() {
        return pattern.getCurrentChunkZ() + lobe.getOffsetChunkZ();
    }

    public float getFadeAlpha() {
        return pattern.getFadeAlpha();
    }

    public boolean isRetiring() {
        return pattern.isRetiring();
    }

    public float getIntensity() {
        return pattern.getIntensity();
    }
}