package application.bootstrap.weatherpipeline.seasonmanager;

import java.io.File;

import com.google.gson.JsonObject;

import application.bootstrap.weatherpipeline.season.Season;
import application.bootstrap.weatherpipeline.season.SeasonData;
import application.bootstrap.weatherpipeline.season.SeasonHandle;
import engine.root.BuilderPackage;
import engine.util.io.FileUtility;
import engine.util.io.JsonUtility;

class InternalBuilder extends BuilderPackage {

    /*
     * Parses season JSON into a SeasonData and wraps it in a SeasonHandle.
     * The filename (without extension) must match a Season enum constant
     * exactly — e.g. "SPRING.json" resolves to Season.SPRING. Bootstrap-only.
     */

    // Build \\

    SeasonHandle build(File file, File root) {

        String resourceName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        Season season = resolveSeason(resourceName, file);

        JsonObject json = JsonUtility.loadJsonObject(file);

        float baseWindSpeed = parseFloat(json, "baseWindSpeed", 3.0f);
        float windVariance = parseFloat(json, "windVariance", 1.0f);
        float prevailingWindDirectionDegrees = parseFloat(json, "prevailingWindDirectionDegrees", 0.0f);
        float baseTemperature = parseFloat(json, "baseTemperature", 15.0f);
        float temperatureVariance = parseFloat(json, "temperatureVariance", 5.0f);
        float precipitationChanceScale = parseFloat(json, "precipitationChanceScale", 1.0f);

        SeasonData seasonData = new SeasonData(
                season,
                baseWindSpeed,
                windVariance,
                prevailingWindDirectionDegrees,
                baseTemperature,
                temperatureVariance,
                precipitationChanceScale);

        SeasonHandle seasonHandle = create(SeasonHandle.class);
        seasonHandle.constructor(seasonData);

        return seasonHandle;
    }

    // Parsing \\

    private Season resolveSeason(String resourceName, File file) {

        try {
            return Season.valueOf(resourceName);
        } catch (IllegalArgumentException e) {
            return throwException("Season file name must match a Season enum constant "
                    + "(SPRING, SUMMER, FALL, WINTER): " + file.getName());
        }
    }

    private float parseFloat(JsonObject json, String field, float fallback) {

        if (!json.has(field))
            return fallback;

        return json.get(field).getAsFloat();
    }
}