package application.bootstrap.weatherpipeline.weathermanager;

import application.bootstrap.weatherpipeline.weather.WeatherHandle;
import engine.root.StructPackage;
import engine.util.random.ChanceWeighted;

class WeatherPoolEntryStruct extends StructPackage implements ChanceWeighted {

    /*
     * Resolved pairing of a live WeatherHandle and its relative chance
     * weight within the current biome/season pool. Built once per season
     * change by WeatherManager.resolveWeatherPool() and consumed by
     * RegionSampleBranch for chance-weighted, noise-blended sampling.
     */

    // Internal
    private final WeatherHandle weatherHandle;
    private final float chance;

    // Constructor \\

    WeatherPoolEntryStruct(WeatherHandle weatherHandle, float chance) {

        // Internal
        this.weatherHandle = weatherHandle;
        this.chance = chance;
    }

    // Accessible \\

    WeatherHandle getWeatherHandle() {
        return weatherHandle;
    }

    @Override
    public float getChance() {
        return chance;
    }
}