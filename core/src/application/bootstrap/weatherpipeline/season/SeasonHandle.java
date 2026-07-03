package application.bootstrap.weatherpipeline.season;

import engine.root.HandlePackage;

public class SeasonHandle extends HandlePackage {

    /*
     * Persistent climate record for one season. Wraps SeasonData and
     * delegates all access through it. Registered in SeasonManager from
     * bootstrap to shutdown — all four seasons load eagerly, never on
     * demand.
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

    public Season getSeason() {
        return seasonData.getSeason();
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