package application.bootstrap.entitypipeline.entitymanager;

import java.io.File;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import application.bootstrap.entitypipeline.appearance.AppearanceData;
import application.bootstrap.entitypipeline.feature.FeatureHandle;
import application.bootstrap.entitypipeline.feature.FeatureSlot;
import application.bootstrap.entitypipeline.featuremanager.FeatureManager;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import engine.graphics.color.Color;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.io.JsonUtility;

class AppearanceBuilder extends BuilderPackage {

    /*
     * Parses the "appearance" block of an entity template's "model" into an
     * AppearanceData, resolved against the character mesh's rig. Every
     * default feature must sit in the slot it is declared under, fit the
     * rig and texture array (AppearanceData.isCompatible), and every
     * required slot must be filled. Colors default to white and the build
     * curve to a flat 1.0 when omitted. Bootstrap-only.
     */

    // Internal
    private FeatureManager featureManager;

    // Base \\

    @Override
    protected void get() {
        this.featureManager = get(FeatureManager.class);
    }

    // Build \\

    AppearanceData build(JsonObject appearanceJson, RigHandle rigHandle, File file) {

        String headBoneName = JsonUtility.validateString(appearanceJson, "head_bone");

        if (!rigHandle.hasBone(headBoneName))
            throwException("Appearance \"head_bone\" \"" + headBoneName
                    + "\" is not a bone of the character rig. File: " + file.getName());

        FeatureHandle[] defaultFeatures = parseFeatures(JsonUtility.validateObject(appearanceJson, "features"), file);
        JsonObject buildJson = JsonUtility.hasObject(appearanceJson, "build")
                ? appearanceJson.getAsJsonObject("build")
                : new JsonObject();

        AppearanceData appearanceData = new AppearanceData(
                rigHandle,
                rigHandle.getBoneIndex(headBoneName),
                parseColor(appearanceJson, "skin_color"),
                parseColor(appearanceJson, "hair_color"),
                defaultFeatures,
                JsonUtility.getFloat(buildJson, "thin", EngineSetting.DEFAULT_BUILD_FACTOR),
                JsonUtility.getFloat(buildJson, "heavy", EngineSetting.DEFAULT_BUILD_FACTOR),
                parseGirthInfluence(buildJson, rigHandle, file));

        for (FeatureHandle featureHandle : defaultFeatures)
            if (featureHandle != null && !appearanceData.isCompatible(featureHandle))
                throwException("Default feature \"" + featureHandle.getFeatureName()
                        + "\" does not fit this character — its mesh must use the character's rig and its "
                        + "textures must share the head's face texture array. File: " + file.getName());

        return appearanceData;
    }

    // Features \\

    private FeatureHandle[] parseFeatures(JsonObject featuresJson, File file) {

        FeatureHandle[] features = new FeatureHandle[FeatureSlot.values().length];

        for (FeatureSlot featureSlot : FeatureSlot.values()) {

            String key = featureSlot.name().toLowerCase();

            if (!JsonUtility.hasString(featuresJson, key)) {

                if (featureSlot.isRequired())
                    throwException("Appearance is missing required feature \"" + key + "\". File: " + file.getName());

                continue;
            }

            FeatureHandle featureHandle = featureManager.getFeatureHandleFromFeatureName(
                    featuresJson.get(key).getAsString());

            if (featureHandle.getFeatureSlot() != featureSlot)
                throwException("Feature \"" + featureHandle.getFeatureName() + "\" is a "
                        + featureHandle.getFeatureSlot() + " feature but is declared under \"" + key
                        + "\". File: " + file.getName());

            features[featureSlot.ordinal()] = featureHandle;
        }

        for (String key : featuresJson.keySet())
            if (!isSlotName(key))
                throwException("Appearance declares unknown feature slot \"" + key + "\". File: " + file.getName());

        return features;
    }

    private boolean isSlotName(String key) {

        for (FeatureSlot featureSlot : FeatureSlot.values())
            if (featureSlot.name().equalsIgnoreCase(key))
                return true;

        return false;
    }

    // Build Curve \\

    private float[] parseGirthInfluence(JsonObject buildJson, RigHandle rigHandle, File file) {

        float[] girthInfluence = new float[rigHandle.getBoneCount()];

        if (!JsonUtility.hasObject(buildJson, "bones"))
            return girthInfluence;

        JsonObject bonesJson = buildJson.getAsJsonObject("bones");

        for (String boneName : bonesJson.keySet()) {

            if (!rigHandle.hasBone(boneName))
                throwException("Appearance build references unknown bone \"" + boneName + "\". File: "
                        + file.getName());

            girthInfluence[rigHandle.getBoneIndex(boneName)] = bonesJson.get(boneName).getAsFloat();
        }

        return girthInfluence;
    }

    // Color \\

    private Color parseColor(JsonObject json, String key) {

        if (!JsonUtility.hasArray(json, key))
            return new Color(Color.WHITE);

        JsonArray color = JsonUtility.validateArray(json, key, 3);

        return new Color(
                color.get(0).getAsFloat(),
                color.get(1).getAsFloat(),
                color.get(2).getAsFloat());
    }
}
