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
     * field falls back to one of EngineSetting's DEFAULT_CLOUD_* constants
     * when omitted — no fallback value is authored inline here. "scale" is
     * the cloud's full XZ width in blocks; CloudVolumeMesh is a literal
     * 1x1x1 unit cube multiplied directly by it, with no other implicit
     * base size.
     */

    CloudHandle build(File file, File root) {

        String cloudName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        short cloudID = RegistryUtility.toShortID(cloudName);

        JsonObject json = JsonUtility.loadJsonObject(file);

        Vector3 cloudColor = parseColor(json, "color", new Vector3(
                EngineSetting.DEFAULT_CLOUD_COLOR_R,
                EngineSetting.DEFAULT_CLOUD_COLOR_G,
                EngineSetting.DEFAULT_CLOUD_COLOR_B));
        float saturation = parseUnitFloat(json, cloudName, "saturation", EngineSetting.DEFAULT_CLOUD_SATURATION);
        float scale = parseFloat(json, "scale", EngineSetting.CLOUD_DEFAULT_DIAMETER_BLOCKS);
        float density = parseFloat(json, "density", EngineSetting.DEFAULT_CLOUD_DENSITY);
        float verticalThickness = parseFloat(
                json, "verticalThickness", EngineSetting.DEFAULT_CLOUD_VERTICAL_THICKNESS_BLOCKS);
        float fullness = parseUnitFloat(json, cloudName, "fullness", EngineSetting.DEFAULT_CLOUD_FULLNESS);
        float densityNoiseScale = parseFloat(
                json, "densityNoiseScale", EngineSetting.DEFAULT_CLOUD_DENSITY_NOISE_SCALE);
        float noiseWarpStrength = parseFloat(
                json, "noiseWarpStrength", EngineSetting.DEFAULT_CLOUD_NOISE_WARP_STRENGTH);
        float coverageBias = parseFloat(json, "coverageBias", EngineSetting.DEFAULT_CLOUD_COVERAGE_BIAS);
        float silhouetteSoftness = parseFloat(
                json, "silhouetteSoftness", EngineSetting.DEFAULT_CLOUD_SILHOUETTE_SOFTNESS);
        float baseAltitude = parseFloat(json, "baseAltitude", EngineSetting.DEFAULT_CLOUD_BASE_ALTITUDE_BLOCKS);
        float driftSpeedScale = parseFloat(json, "driftSpeedScale", EngineSetting.DEFAULT_CLOUD_DRIFT_SPEED_SCALE);
        float spreadRatio = parseFloat(json, "spread", EngineSetting.DEFAULT_CLOUD_SPREAD_RATIO);
        float sizeVarianceMin = parseFloat(json, "sizeVarianceMin", EngineSetting.DEFAULT_CLOUD_SIZE_VARIANCE_MIN);
        float sizeVarianceMax = parseFloat(json, "sizeVarianceMax", EngineSetting.DEFAULT_CLOUD_SIZE_VARIANCE_MAX);
        float elongationMin = parseFloat(json, "elongationMin", EngineSetting.DEFAULT_CLOUD_ELONGATION_MIN);
        float elongationMax = parseFloat(json, "elongationMax", EngineSetting.DEFAULT_CLOUD_ELONGATION_MAX);

        CloudData cloudData = new CloudData(
                cloudName,
                cloudID,
                cloudColor,
                saturation,
                scale,
                density,
                verticalThickness,
                fullness,
                densityNoiseScale,
                noiseWarpStrength,
                coverageBias,
                silhouetteSoftness,
                baseAltitude,
                driftSpeedScale,
                spreadRatio,
                sizeVarianceMin,
                sizeVarianceMax,
                elongationMin,
                elongationMax);

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

        if (!json.has(field))
            return fallback;

        float value = json.get(field).getAsFloat();

        if (value < 0f || value > 1f)
            throwException("Cloud \"" + cloudName + "\" field \"" + field
                    + "\" must be between 0.0 and 1.0, got: " + value);

        return value;
    }

    private Vector3 parseColor(JsonObject json, String field, Vector3 fallback) {

        if (!json.has(field))
            return fallback;

        JsonObject colorObject = json.getAsJsonObject(field);

        float r = colorObject.get("r").getAsFloat();
        float g = colorObject.get("g").getAsFloat();
        float b = colorObject.get("b").getAsFloat();

        return new Vector3(r, g, b);
    }
}