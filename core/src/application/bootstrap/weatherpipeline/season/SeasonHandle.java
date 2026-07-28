package application.bootstrap.weatherpipeline.season;

import engine.root.HandlePackage;
import engine.util.mathematics.vectors.Vector3;

public class SeasonHandle extends HandlePackage {

    /*
     * Persistent climate and sky-color record for one named season. Wraps
     * SeasonData and delegates all access through it. Registered in
     * SeasonManager from bootstrap to shutdown, keyed by name — the same
     * open-ended registry pattern as clouds, weathers, and biomes, since
     * the set of seasons is entirely defined by whichever calendar a
     * world uses.
     */

    private SeasonData seasonData;

    public void constructor(SeasonData seasonData) {
        this.seasonData = seasonData;
    }

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

    public Vector3 getTintColor() {
        return seasonData.getTintColor();
    }

    public Vector3 getSunriseColor() {
        return seasonData.getSunriseColor();
    }
}