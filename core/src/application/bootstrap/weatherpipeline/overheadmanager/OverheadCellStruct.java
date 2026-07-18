package application.bootstrap.weatherpipeline.overheadmanager;

import application.bootstrap.weatherpipeline.cloud.CloudHandle;
import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import application.bootstrap.weatherpipeline.weatherpatternmanager.WeatherPatternLobeStruct;
import application.bootstrap.weatherpipeline.weatherpatternmanager.WeatherPatternStruct;
import engine.root.StructPackage;

public class OverheadCellStruct extends StructPackage {

    /*
     * One renderable cloud-volume placement — a thin read-through view of
     * one lobe belonging to one WeatherPatternStruct. Every getter
     * delegates to the owning pattern and/or lobe, so position, size, fade,
     * and intensity always track whatever the pattern most recently
     * resolved. Position and size specifically route through the pattern's
     * own getLobeChunkX/Z/SizeVariance so a lobe's on-screen placement
     * always reflects the pattern's current band purity, not just its raw
     * fixed offset.
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
        return pattern.getLobeSizeVariance(lobe);
    }

    public float getDomainRotation() {
        return lobe.getDomainRotation();
    }

    public float getElongation() {
        return lobe.getElongation();
    }

    public double getCurrentChunkX() {
        return pattern.getLobeChunkX(lobe);
    }

    public double getCurrentChunkZ() {
        return pattern.getLobeChunkZ(lobe);
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