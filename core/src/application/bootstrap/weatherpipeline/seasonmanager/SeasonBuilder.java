package application.bootstrap.weatherpipeline.seasonmanager;

import java.io.File;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import application.bootstrap.weatherpipeline.season.SeasonData;
import application.bootstrap.weatherpipeline.season.SeasonHandle;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.io.FileUtility;
import engine.util.io.JsonUtility;
import engine.util.mathematics.vectors.Vector3;

class SeasonBuilder extends BuilderPackage {

    /*
     * Parses season JSON into a SeasonData and wraps it in a SeasonHandle.
     * The season's name is taken directly from the file name — whatever
     * named seasons the active calendar defines is whatever files should
     * exist here. Every fallback below when a JSON field is omitted comes
     * from EngineSetting — nothing is authored as a bare literal here.
     * Bootstrap-only and on-demand.
     */

    SeasonHandle build(File file, File root) {

        String seasonName = FileUtility.getPathWithFileNameWithoutExtension(root, file);

        JsonObject json = JsonUtility.loadJsonObject(file);

        float baseWindSpeed = parseFloat(json, "baseWindSpeed", EngineSetting.DEFAULT_SEASON_BASE_WIND_SPEED);
        float windVariance = parseFloat(json, "windVariance", EngineSetting.DEFAULT_SEASON_WIND_VARIANCE);
        float prevailingWindDirectionDegrees = parseFloat(
                json, "prevailingWindDirectionDegrees",
                EngineSetting.DEFAULT_SEASON_PREVAILING_WIND_DIRECTION_DEGREES);
        float baseTemperature = parseFloat(json, "baseTemperature", EngineSetting.DEFAULT_BASE_TEMPERATURE);
        float temperatureVariance = parseFloat(
                json, "temperatureVariance", EngineSetting.DEFAULT_SEASON_TEMPERATURE_VARIANCE);
        float precipitationChanceScale = parseFloat(
                json, "precipitationChanceScale", EngineSetting.DEFAULT_SEASON_PRECIPITATION_CHANCE_SCALE);
        Vector3 tintColor = parseColor(json, "tintColor", new Vector3(
                EngineSetting.DEFAULT_SEASON_TINT_R,
                EngineSetting.DEFAULT_SEASON_TINT_G,
                EngineSetting.DEFAULT_SEASON_TINT_B));
        Vector3 sunriseColor = parseColor(json, "sunriseColor", new Vector3(
                EngineSetting.DEFAULT_SEASON_SUNRISE_R,
                EngineSetting.DEFAULT_SEASON_SUNRISE_G,
                EngineSetting.DEFAULT_SEASON_SUNRISE_B));

        SeasonData seasonData = new SeasonData(
                seasonName,
                baseWindSpeed,
                windVariance,
                prevailingWindDirectionDegrees,
                baseTemperature,
                temperatureVariance,
                precipitationChanceScale,
                tintColor,
                sunriseColor);

        SeasonHandle seasonHandle = create(SeasonHandle.class);
        seasonHandle.constructor(seasonData);

        return seasonHandle;
    }

    private float parseFloat(JsonObject json, String field, float fallback) {

        if (!json.has(field))
            return fallback;

        return json.get(field).getAsFloat();
    }

    private Vector3 parseColor(JsonObject json, String field, Vector3 fallback) {

        if (!json.has(field))
            return fallback;

        JsonArray array = json.getAsJsonArray(field);

        return new Vector3(
                array.get(0).getAsFloat(),
                array.get(1).getAsFloat(),
                array.get(2).getAsFloat());
    }
}