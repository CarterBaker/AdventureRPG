package application.bootstrap.weatherpipeline.weather;

import engine.root.StructPackage;

public class NextWeatherChanceStruct extends StructPackage {

    /*
     * One suggested successor within a weather's own "possible next
     * weather" list — a name and a relative chance. Kept as a bare name
     * rather than a resolved handle: resolving it at bootstrap time would
     * let two weathers that suggest each other deadlock the loader, since
     * each would try to force-load the other mid-build. Consulted only as a
     * bias on top of the biome/season pool's own noise-driven pick — never
     * a guarantee.
     */

    private final String weatherName;
    private final float chance;

    public NextWeatherChanceStruct(String weatherName, float chance) {
        this.weatherName = weatherName;
        this.chance = chance;
    }

    public String getWeatherName() {
        return weatherName;
    }

    public float getChance() {
        return chance;
    }
}