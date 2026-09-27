package application.bootstrap.entitypipeline.entitymanager;

import java.io.File;

import application.bootstrap.entitypipeline.appearance.AppearanceData;
import application.bootstrap.entitypipeline.feature.FeatureHandle;
import application.bootstrap.entitypipeline.feature.FeatureSlot;
import application.bootstrap.entitypipeline.featuremanager.FeatureManager;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import engine.graphics.color.Color;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;

class AppearanceBuilder extends BuilderPackage {

    /*
     * Parses the "appearance" block of an entity template's "model" into an
     * AppearanceData, resolved against the character mesh's rig. Every
     * default feature must sit in the slot it is declared under, fit the
     * rig and texture array (AppearanceData.isCompatible), and every
     * required slot must be filled. Colors default to white and the build
     * curve to a flat 1.0 when omitted; a palette left out holds only its
     * default color. Bootstrap-only.
     */

    // Internal
    private FeatureManager featureManager;

    // Base \\

    @Override
    protected void get() {
        this.featureManager = get(FeatureManager.class);
    }

    // Build \\

    AppearanceData build(ArpgObjectStruct appearanceArpg, RigHandle rigHandle, File file) {

        String headBoneName = ArpgUtility.validateString(appearanceArpg, "head_bone");

        if (!rigHandle.hasBone(headBoneName))
            throwException("Appearance \"head_bone\" \"" + headBoneName
                    + "\" is not a bone of the character rig. File: " + file.getName());

        FeatureHandle[] defaultFeatures = parseFeatures(ArpgUtility.validateObject(appearanceArpg, "features"), file);
        Color skinColor = parseColor(appearanceArpg, "skin_color");
        Color hairColor = parseColor(appearanceArpg, "hair_color");
        ArpgObjectStruct buildArpg = ArpgUtility.hasObject(appearanceArpg, "build")
                ? appearanceArpg.getAsObject("build")
                : new ArpgObjectStruct();

        AppearanceData appearanceData = new AppearanceData(
                rigHandle,
                rigHandle.getBoneIndex(headBoneName),
                skinColor,
                hairColor,
                parsePalette(appearanceArpg, "skin_palette", skinColor, file),
                parsePalette(appearanceArpg, "hair_palette", hairColor, file),
                defaultFeatures,
                ArpgUtility.getFloat(buildArpg, "thin", EngineSetting.DEFAULT_BUILD_FACTOR),
                ArpgUtility.getFloat(buildArpg, "heavy", EngineSetting.DEFAULT_BUILD_FACTOR),
                parseGirthInfluence(buildArpg, rigHandle, file));

        for (FeatureHandle featureHandle : defaultFeatures)
            if (featureHandle != null && !appearanceData.isCompatible(featureHandle))
                throwException("Default feature \"" + featureHandle.getFeatureName()
                        + "\" does not fit this character — its mesh must use the character's rig and its "
                        + "textures must share the head's face texture array. File: " + file.getName());

        return appearanceData;
    }

    // Features \\

    private FeatureHandle[] parseFeatures(ArpgObjectStruct featuresArpg, File file) {

        FeatureHandle[] features = new FeatureHandle[FeatureSlot.VALUES.length];

        for (FeatureSlot featureSlot : FeatureSlot.VALUES) {

            String key = featureSlot.name().toLowerCase();

            if (!ArpgUtility.hasString(featuresArpg, key)) {

                if (featureSlot.isRequired())
                    throwException("Appearance is missing required feature \"" + key + "\". File: " + file.getName());

                continue;
            }

            FeatureHandle featureHandle = featureManager.getFeatureHandleFromFeatureName(
                    featuresArpg.get(key).getAsString());

            if (featureHandle.getFeatureSlot() != featureSlot)
                throwException("Feature \"" + featureHandle.getFeatureName() + "\" is a "
                        + featureHandle.getFeatureSlot() + " feature but is declared under \"" + key
                        + "\". File: " + file.getName());

            features[featureSlot.ordinal()] = featureHandle;
        }

        for (String key : featuresArpg.keySet())
            if (!isSlotName(key))
                throwException("Appearance declares unknown feature slot \"" + key + "\". File: " + file.getName());

        return features;
    }

    private boolean isSlotName(String key) {

        for (FeatureSlot featureSlot : FeatureSlot.VALUES)
            if (featureSlot.name().equalsIgnoreCase(key))
                return true;

        return false;
    }

    // Build Curve \\

    private float[] parseGirthInfluence(ArpgObjectStruct buildArpg, RigHandle rigHandle, File file) {

        float[] girthInfluence = new float[rigHandle.getBoneCount()];

        if (!ArpgUtility.hasObject(buildArpg, "bones"))
            return girthInfluence;

        ArpgObjectStruct bonesArpg = buildArpg.getAsObject("bones");

        for (String boneName : bonesArpg.keySet()) {

            if (!rigHandle.hasBone(boneName))
                throwException("Appearance build references unknown bone \"" + boneName + "\". File: "
                        + file.getName());

            girthInfluence[rigHandle.getBoneIndex(boneName)] = bonesArpg.get(boneName).getAsFloat();
        }

        return girthInfluence;
    }

    // Color \\

    private Color parseColor(ArpgObjectStruct arpg, String key) {

        if (!ArpgUtility.hasArray(arpg, key))
            return new Color(Color.WHITE);

        return toColor(ArpgUtility.validateArray(arpg, key, 3));
    }

    private Color[] parsePalette(ArpgObjectStruct arpg, String key, Color defaultColor, File file) {

        if (!ArpgUtility.hasArray(arpg, key))
            return new Color[] { new Color(defaultColor) };

        ArpgArrayStruct paletteArpg = arpg.getAsArray(key);

        if (paletteArpg.size() == 0)
            throwException("Appearance \"" + key + "\" must list at least one color. File: " + file.getName());

        Color[] palette = new Color[paletteArpg.size()];

        for (int i = 0; i < palette.length; i++) {

            ArpgArrayStruct color = paletteArpg.get(i).getAsArray();

            if (color.size() != 3)
                throwException("Appearance \"" + key + "\" entry " + i + " must be [r, g, b]. File: " + file.getName());

            palette[i] = toColor(color);
        }

        return palette;
    }

    private Color toColor(ArpgArrayStruct color) {
        return new Color(
                color.get(0).getAsFloat(),
                color.get(1).getAsFloat(),
                color.get(2).getAsFloat());
    }
}
