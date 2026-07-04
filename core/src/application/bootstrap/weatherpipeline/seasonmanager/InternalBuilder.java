package application.bootstrap.weatherpipeline.seasonmanager;

import java.io.File;

import com.google.gson.JsonObject;

import application.bootstrap.weatherpipeline.season.SeasonData;
import application.bootstrap.weatherpipeline.season.SeasonHandle;
import engine.root.BuilderPackage;
import engine.util.io.FileUtility;
import engine.util.io.JsonUtility;

class InternalBuilder extends BuilderPackage {

    /*
     * Parses season JSON into a SeasonData and wraps it in a SeasonHandle.
     * The season's name is taken directly from the file name — whatever
     * named seasons the active calendar defines is whatever files should
     * exist here (e.g. "Spring.json", "Summer.json", "Autumn.json",
     * "Winter.json", or entirely different names for an alien calendar).
     * Bootstrap-only and on-demand.
     */

    // Build \\

    SeasonHandle build(File file, File root) {

        String seasonName = FileUtility.getPathWithFileNameWithoutExtension(root, file);

        JsonObject json = JsonUtility.loadJsonObject(file);

        float baseWindSpeed = parseFloat(json, "baseWindSpeed", 3.0f);
        float windVariance = parseFloat(json, "windVariance", 1.0f);
        float prevailingWindDirectionDegrees = parseFloat(json, "prevailingWindDirectionDegrees", 0.0f);
        float baseTemperature = parseFloat(json, "baseTemperature", 15.0f);
        float temperatureVariance = parseFloat(json, "temperatureVariance", 5.0f);
        float precipitationChanceScale = parseFloat(json, "precipitationChanceScale", 1.0f);

        SeasonData seasonData = new SeasonData(
                seasonName,
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

    private float parseFloat(JsonObject json, String field, float fallback) {

        if (!json.has(field))
            return fallback;

        return json.get(field).getAsFloat();
    }
}