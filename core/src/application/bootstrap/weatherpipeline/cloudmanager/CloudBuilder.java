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
     * field falls back to a sensible default when omitted. "scale" is the
     * cloud's full XZ width in blocks; CloudVolumeMesh is a literal 1x1x1
     * unit cube multiplied directly by it, with no other implicit base size.
     */

    CloudHandle build(File file, File root) {

        String cloudName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        short cloudID = RegistryUtility.toShortID(cloudName);

        JsonObject json = JsonUtility.loadJsonObject(file);

        Vector3 cloudColor = parseColor(json, "color", new Vector3(1f, 1f, 1f));
        float saturation = parseUnitFloat(json, cloudName, "saturation", 1.0f);
        float scale = parseFloat(json, "scale", EngineSetting.CLOUD_DEFAULT_DIAMETER_BLOCKS);
        float density = parseFloat(json, "density", 0.8f);
        float verticalThickness = parseFloat(json, "verticalThickness", 8.0f);
        float fullness = parseUnitFloat(json, cloudName, "fullness", 0.7f);
        float densityNoiseScale = parseFloat(json, "densityNoiseScale", 1.0f);
        float noiseWarpStrength = parseFloat(json, "noiseWarpStrength", 0.6f);
        float coverageBias = parseFloat(json, "coverageBias", 0.5f);
        float silhouetteSoftness = parseFloat(json, "silhouetteSoftness", 0.08f);
        float baseAltitude = parseFloat(json, "baseAltitude", 128.0f);
        float driftSpeedScale = parseFloat(json, "driftSpeedScale", 1.0f);
        float spreadRatio = parseFloat(json, "spread", 0.85f);
        float sizeVarianceMin = parseFloat(json, "sizeVarianceMin", 0.65f);
        float sizeVarianceMax = parseFloat(json, "sizeVarianceMax", 1.6f);
        float elongationMin = parseFloat(json, "elongationMin", 1.0f);
        float elongationMax = parseFloat(json, "elongationMax", 2.4f);

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