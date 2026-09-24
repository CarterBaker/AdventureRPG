package application.bootstrap.weatherpipeline.cloudmanager;

import java.io.File;

import com.google.gson.JsonObject;

import application.bootstrap.weatherpipeline.cloud.CloudData;
import application.bootstrap.weatherpipeline.cloud.CloudHandle;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.io.FileUtility;
import engine.util.io.JsonUtility;
import engine.util.mathematics.vectors.Vector3;
import engine.util.registry.RegistryUtility;

class CloudBuilder extends BuilderPackage {

    /*
     * Parses cloud JSON into a CloudData and wraps it in a CloudHandle. Every
     * field falls back to its EngineSetting default when omitted. "scale" is
     * the archetype's feature width in blocks, "elongation" stretches that
     * width along the prevailing flow, and "baseAltitude" plus
     * "verticalThickness" place the archetype's layer in the sky.
     */

    // Build \\

    CloudHandle build(File file, File root) {

        String cloudName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        short cloudID = RegistryUtility.toShortID(cloudName);

        JsonObject json = JsonUtility.loadJsonObject(file);

        Vector3 cloudColor = parseColor(json, "color");
        float saturation = parseUnitFloat(json, cloudName, "saturation", EngineSetting.DEFAULT_CLOUD_SATURATION);
        float scale = parsePositiveFloat(json, cloudName, "scale", EngineSetting.CLOUD_DEFAULT_DIAMETER_BLOCKS);
        float density = parseFloat(json, "density", EngineSetting.DEFAULT_CLOUD_DENSITY);
        float verticalThickness = parsePositiveFloat(
                json, cloudName, "verticalThickness", EngineSetting.DEFAULT_CLOUD_VERTICAL_THICKNESS);
        float fullness = parseUnitFloat(json, cloudName, "fullness", EngineSetting.DEFAULT_CLOUD_FULLNESS);
        float elongation = parsePositiveFloat(json, cloudName, "elongation", EngineSetting.DEFAULT_CLOUD_ELONGATION);
        float densityNoiseScale = parseFloat(
                json, "densityNoiseScale", EngineSetting.DEFAULT_CLOUD_DENSITY_NOISE_SCALE);
        float noiseWarpStrength = parseFloat(
                json, "noiseWarpStrength", EngineSetting.DEFAULT_CLOUD_NOISE_WARP_STRENGTH);
        float coverageBias = parseUnitFloat(json, cloudName, "coverageBias", EngineSetting.DEFAULT_CLOUD_COVERAGE_BIAS);
        float silhouetteSoftness = parseFloat(
                json, "silhouetteSoftness", EngineSetting.DEFAULT_CLOUD_SILHOUETTE_SOFTNESS);
        float baseAltitude = parseFloat(json, "baseAltitude", EngineSetting.DEFAULT_CLOUD_BASE_ALTITUDE);
        float driftSpeedScale = parseFloat(json, "driftSpeedScale", EngineSetting.DEFAULT_CLOUD_DRIFT_SPEED_SCALE);

        CloudData cloudData = new CloudData(
                cloudName,
                cloudID,
                cloudColor,
                saturation,
                scale,
                density,
                verticalThickness,
                fullness,
                elongation,
                densityNoiseScale,
                noiseWarpStrength,
                coverageBias,
                silhouetteSoftness,
                baseAltitude,
                driftSpeedScale);

        CloudHandle cloudHandle = create(CloudHandle.class);
        cloudHandle.constructor(cloudData);

        return cloudHandle;
    }

    // Parsing \\

    private float parseFloat(JsonObject json, String field, float fallback) {

        if (!json.has(field))
            return fallback;

        return json.get(field).getAsFloat();
    }

    private float parseUnitFloat(JsonObject json, String cloudName, String field, float fallback) {

        float value = parseFloat(json, field, fallback);

        if (value < 0f || value > 1f)
            throwException("Cloud \"" + cloudName + "\" field \"" + field
                    + "\" must be between 0.0 and 1.0, got: " + value);

        return value;
    }

    private float parsePositiveFloat(JsonObject json, String cloudName, String field, float fallback) {

        float value = parseFloat(json, field, fallback);

        if (value <= 0f)
            throwException("Cloud \"" + cloudName + "\" field \"" + field + "\" must be greater than 0.0, got: "
                    + value);

        return value;
    }

    private Vector3 parseColor(JsonObject json, String field) {

        if (!json.has(field))
            return new Vector3(
                    EngineSetting.DEFAULT_CLOUD_COLOR_R,
                    EngineSetting.DEFAULT_CLOUD_COLOR_G,
                    EngineSetting.DEFAULT_CLOUD_COLOR_B);

        JsonObject colorObject = json.getAsJsonObject(field);

        float r = colorObject.get("r").getAsFloat();
        float g = colorObject.get("g").getAsFloat();
        float b = colorObject.get("b").getAsFloat();

        return new Vector3(r, g, b);
    }
}
