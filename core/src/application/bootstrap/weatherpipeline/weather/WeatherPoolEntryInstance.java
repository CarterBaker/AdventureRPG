package application.bootstrap.weatherpipeline.weather;

import engine.root.InstancePackage;
import engine.util.random.ChanceWeighted;

public class WeatherPoolEntryInstance extends InstancePackage implements ChanceWeighted {

    /*
     * Resolved pairing of a live WeatherHandle and its relative chance
     * weight within a biome/season pool, or a biased "next weather" pool
     * built on top of it. Consumed by RegionSampleSystem for chance-
     * weighted, noise-blended sampling. WeatherManager holds persistent,
     * growable pools of these and re-arms them via constructor() rather
     * than allocating fresh entries per season change or reevaluation.
     */

    private WeatherHandle weatherHandle;
    private float chance;

    // Constructor \\

    public void constructor(WeatherHandle weatherHandle, float chance) {
        this.weatherHandle = weatherHandle;
        this.chance = chance;
    }

    // Accessible \\

    public WeatherHandle getWeatherHandle() {
        return weatherHandle;
    }

    @Override
    public float getChance() {
        return chance;
    }
}