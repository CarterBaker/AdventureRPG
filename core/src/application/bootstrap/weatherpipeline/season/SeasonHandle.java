package application.bootstrap.weatherpipeline.season;

import engine.root.HandlePackage;

public class SeasonHandle extends HandlePackage {

    /*
     * Persistent climate record for one named season. Wraps SeasonData and
     * delegates all access through it. Registered in SeasonManager from
     * bootstrap to shutdown, keyed by name — the same open-ended registry
     * pattern as clouds, weathers, and biomes, since the set of seasons is
     * entirely defined by whichever calendar a world uses.
     */

    // Internal
    private SeasonData seasonData;

    // Constructor \\

    public void constructor(SeasonData seasonData) {
        this.seasonData = seasonData;
    }

    // Accessible \\

    public SeasonData getSeasonData() {
        return seasonData;
    }

    public String getSeasonName() {
        return seasonData.getSeasonName();
    }

    public float getBaseWindSpeed() {
        return seasonData.getBaseWindSpeed();
    }

    public float getWindVariance() {
        return seasonData.getWindVariance();
    }

    public float getPrevailingWindDirectionDegrees() {
        return seasonData.getPrevailingWindDirectionDegrees();
    }

    public float getBaseTemperature() {
        return seasonData.getBaseTemperature();
    }

    public float getTemperatureVariance() {
        return seasonData.getTemperatureVariance();
    }

    public float getPrecipitationChanceScale() {
        return seasonData.getPrecipitationChanceScale();
    }
}