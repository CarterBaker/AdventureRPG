package application.bootstrap.weatherpipeline.cloudmanager;

import java.io.File;

import application.bootstrap.weatherpipeline.cloud.CloudData;
import application.bootstrap.weatherpipeline.cloud.CloudHandle;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import engine.util.mathematics.vectors.Vector3;

class CloudBuilder extends BuilderPackage {

    /*
     * Parses cloud ARPG into a CloudData and wraps it in a CloudHandle. Every
     * field falls back to its EngineSetting default when omitted.
     * "scaleKm" is the archetype's feature width, "verticalThicknessKm" its
     * depth, and "baseAltitudeKm" its base above sea level, all in real-world
     * kilometres so the same archetype sits correctly in a world of any
     * scale. "elongation" stretches the width along the prevailing flow.
     */

    // Internal
    private CloudManager cloudManager;

    // Base \\

    @Override
    protected void get() {
        this.cloudManager = get(CloudManager.class);
    }

    // Build \\

    CloudHandle build(File file, File root) {

        String cloudName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        short cloudID = cloudManager.registerCloudName(cloudName);

        ArpgObjectStruct arpg = ArpgUtility.loadObject(file);

        Vector3 cloudColor = parseColor(arpg, "color");
        float saturation = parseUnitFloat(arpg, cloudName, "saturation", EngineSetting.DEFAULT_CLOUD_SATURATION);
        float scaleKm = parsePositiveFloat(arpg, cloudName, "scaleKm", EngineSetting.DEFAULT_CLOUD_SCALE_KM);
        float density = parseFloat(arpg, "density", EngineSetting.DEFAULT_CLOUD_DENSITY);
        float verticalThicknessKm = parsePositiveFloat(
                arpg, cloudName, "verticalThicknessKm", EngineSetting.DEFAULT_CLOUD_VERTICAL_THICKNESS_KM);
        float fullness = parseUnitFloat(arpg, cloudName, "fullness", EngineSetting.DEFAULT_CLOUD_FULLNESS);
        float elongation = parsePositiveFloat(arpg, cloudName, "elongation", EngineSetting.DEFAULT_CLOUD_ELONGATION);
        float densityNoiseScale = parseFloat(
                arpg, "densityNoiseScale", EngineSetting.DEFAULT_CLOUD_DENSITY_NOISE_SCALE);
        float noiseWarpStrength = parseFloat(
                arpg, "noiseWarpStrength", EngineSetting.DEFAULT_CLOUD_NOISE_WARP_STRENGTH);
        float coverageBias = parseUnitFloat(arpg, cloudName, "coverageBias", EngineSetting.DEFAULT_CLOUD_COVERAGE_BIAS);
        float silhouetteSoftness = parseFloat(
                arpg, "silhouetteSoftness", EngineSetting.DEFAULT_CLOUD_SILHOUETTE_SOFTNESS);
        float baseAltitudeKm = parseFloat(arpg, "baseAltitudeKm", EngineSetting.DEFAULT_CLOUD_BASE_ALTITUDE_KM);
        float driftSpeedScale = parseFloat(arpg, "driftSpeedScale", EngineSetting.DEFAULT_CLOUD_DRIFT_SPEED_SCALE);

        CloudData cloudData = new CloudData(
                cloudName,
                cloudID,
                cloudColor,
                saturation,
                scaleKm,
                density,
                verticalThicknessKm,
                fullness,
                elongation,
                densityNoiseScale,
                noiseWarpStrength,
                coverageBias,
                silhouetteSoftness,
                baseAltitudeKm,
                driftSpeedScale);

        CloudHandle cloudHandle = create(CloudHandle.class);
        cloudHandle.constructor(cloudData);

        return cloudHandle;
    }

    // Parsing \\

    private float parseFloat(ArpgObjectStruct arpg, String field, float fallback) {

        if (!arpg.has(field))
            return fallback;

        return arpg.get(field).getAsFloat();
    }

    private float parseUnitFloat(ArpgObjectStruct arpg, String cloudName, String field, float fallback) {

        float value = parseFloat(arpg, field, fallback);

        if (value < 0f || value > 1f)
            throwException("Cloud \"" + cloudName + "\" field \"" + field
                    + "\" must be between 0.0 and 1.0, got: " + value);

        return value;
    }

    private float parsePositiveFloat(ArpgObjectStruct arpg, String cloudName, String field, float fallback) {

        float value = parseFloat(arpg, field, fallback);

        if (value <= 0f)
            throwException("Cloud \"" + cloudName + "\" field \"" + field + "\" must be greater than 0.0, got: "
                    + value);

        return value;
    }

    private Vector3 parseColor(ArpgObjectStruct arpg, String field) {

        if (!arpg.has(field))
            return new Vector3(
                    EngineSetting.DEFAULT_CLOUD_COLOR_R,
                    EngineSetting.DEFAULT_CLOUD_COLOR_G,
                    EngineSetting.DEFAULT_CLOUD_COLOR_B);

        ArpgObjectStruct colorObject = arpg.getAsObject(field);

        float r = colorObject.get("r").getAsFloat();
        float g = colorObject.get("g").getAsFloat();
        float b = colorObject.get("b").getAsFloat();

        return new Vector3(r, g, b);
    }
}
