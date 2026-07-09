package application.bootstrap.weatherpipeline.cloudmanager;

import java.io.File;

import com.google.gson.JsonObject;

import application.bootstrap.weatherpipeline.cloud.CloudData;
import application.bootstrap.weatherpipeline.cloud.CloudHandle;
import engine.root.BuilderPackage;
import engine.util.io.FileUtility;
import engine.util.io.JsonUtility;
import engine.util.mathematics.vectors.Vector3;
import engine.util.registry.RegistryUtility;

class CloudBuilder extends BuilderPackage {

    /*
     * Parses cloud JSON into a CloudData and wraps it in a CloudHandle.
     * Every field falls back to a sensible default when omitted, so a
     * minimal cloud JSON is valid with just a filename. Bootstrap-only
     * and on-demand.
     */

    // Build \\

    CloudHandle build(File file, File root) {

        String cloudName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        short cloudID = RegistryUtility.toShortID(cloudName);

        JsonObject json = JsonUtility.loadJsonObject(file);

        Vector3 cloudColor = parseColor(json, "color", new Vector3(1f, 1f, 1f));
        float scale = parseFloat(json, "scale", 1.0f);
        float density = parseFloat(json, "density", 0.8f);
        float verticalThickness = parseFloat(json, "verticalThickness", 8.0f);
        float edgeSoftness = parseFloat(json, "edgeSoftness", 0.06f);
        float puffJitter = parseFloat(json, "puffJitter", 0.55f);
        float baseAltitude = parseFloat(json, "baseAltitude", 128.0f);
        float driftSpeedScale = parseFloat(json, "driftSpeedScale", 1.0f);

        Vector3 shadowColor = parseColor(json, "shadowColor", new Vector3(0.6f, 0.63f, 0.7f));
        float shadeStrength = parseFloat(json, "shadeStrength", 0.5f);
        float rimLightStrength = parseFloat(json, "rimLightStrength", 0.35f);
        float ambientOcclusionStrength = parseFloat(json, "ambientOcclusionStrength", 0.4f);
        float brightnessMultiplier = parseFloat(json, "brightnessMultiplier", 1.0f);

        CloudData cloudData = new CloudData(
                cloudName,
                cloudID,
                cloudColor,
                scale,
                density,
                verticalThickness,
                edgeSoftness,
                puffJitter,
                baseAltitude,
                driftSpeedScale,
                shadowColor,
                shadeStrength,
                rimLightStrength,
                ambientOcclusionStrength,
                brightnessMultiplier);

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