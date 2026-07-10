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
     * Parses cloud JSON into a CloudData and wraps it in a CloudHandle.
     * Every field falls back to a sensible default when omitted, so a
     * minimal cloud JSON is valid with just a filename. Bootstrap-only
     * and on-demand.
     *
     * "scale" is a real-world measurement, IN BLOCKS — the full XZ width
     * of the cloud object — not a small unitless multiplier. CloudVolumeMesh
     * is a literal 1x1x1 unit cube (see CloudVolumeMesh.json), and
     * CloudVolumeShader.vsh multiplies it directly by u_cloudScale with no
     * other implicit base size, so whatever this field resolves to IS the
     * cloud's footprint in blocks. "verticalThickness" already followed
     * this convention correctly; the default (and every shipped archetype)
     * previously left "scale" at a leftover unitless value of 1.0-3.0,
     * which rendered every cloud object as a 1-3 block speck.
     */

    // Build \\

    CloudHandle build(File file, File root) {

        String cloudName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        short cloudID = RegistryUtility.toShortID(cloudName);

        JsonObject json = JsonUtility.loadJsonObject(file);

        Vector3 cloudColor = parseColor(json, "color", new Vector3(1f, 1f, 1f));
        Vector3 topColor = parseColor(json, "topColor", new Vector3(1f, 1f, 1f));
        float scale = parseFloat(json, "scale", EngineSetting.CLOUD_DEFAULT_DIAMETER_BLOCKS);
        float density = parseFloat(json, "density", 0.8f);
        float verticalThickness = parseFloat(json, "verticalThickness", 8.0f);
        float edgeSoftness = parseFloat(json, "edgeSoftness", 0.06f);
        float puffJitter = parseFloat(json, "puffJitter", 0.55f);
        int toonBands = parseInt(json, "toonBands", 3);
        float densityNoiseScale = parseFloat(json, "densityNoiseScale", 1.0f);
        float noiseWarpStrength = parseFloat(json, "noiseWarpStrength", 0.6f);
        float coverageBias = parseFloat(json, "coverageBias", 0.5f);
        float silhouetteSoftness = parseFloat(json, "silhouetteSoftness", 0.08f);
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
                topColor,
                scale,
                density,
                verticalThickness,
                edgeSoftness,
                puffJitter,
                toonBands,
                densityNoiseScale,
                noiseWarpStrength,
                coverageBias,
                silhouetteSoftness,
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

    private int parseInt(JsonObject json, String field, int fallback) {

        if (!json.has(field))
            return fallback;

        return json.get(field).getAsInt();
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