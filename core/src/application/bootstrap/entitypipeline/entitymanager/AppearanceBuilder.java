package application.bootstrap.entitypipeline.entitymanager;

import application.bootstrap.entitypipeline.appearance.AppearanceData;
import application.bootstrap.entitypipeline.appearance.AppearanceExpression;
import application.bootstrap.entitypipeline.appearance.AppearanceShapeStruct;
import application.bootstrap.entitypipeline.feature.FeatureHandle;
import application.bootstrap.entitypipeline.feature.FeatureSlot;
import application.bootstrap.entitypipeline.featuremanager.FeatureManager;
import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedColor;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedDetail;
import engine.graphics.color.Color;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;

class AppearanceBuilder extends BuilderPackage {

    /*
     * Parses the "appearance" block of an entity template's "model" into an
     * AppearanceData, resolved against the character mesh and its rig.
     * "colors", "palettes" and "details" are keyed by the lower-case
     * SkinnedColor and SkinnedDetail names; "shapes" lists the sliders, each
     * naming a morph of the character mesh with its group and range;
     * "expressions" names the morph each AppearanceExpression plays. Every
     * default feature must sit in the slot it is declared under and fit the
     * rig. A color left out is white, a palette left out holds only its
     * default color, a detail left out is EngineSetting's default, and the
     * build curve defaults to a flat 1.0. Bootstrap-only.
     */

    // Internal
    private FeatureManager featureManager;

    // Base \\

    @Override
    protected void get() {
        this.featureManager = get(FeatureManager.class);
    }

    // Build \\

    AppearanceData build(ArpgObjectStruct appearanceArpg, MeshHandle characterMesh, String sourceName) {

        RigHandle rigHandle = characterMesh.getRigHandle();
        String headBoneName = ArpgUtility.validateString(appearanceArpg, "head_bone");

        if (!rigHandle.hasBone(headBoneName))
            throwException("Appearance \"head_bone\" \"" + headBoneName
                    + "\" is not a bone of the character rig. Source: " + sourceName);

        Color[] colors = parseColors(appearanceArpg, sourceName);
        ArpgObjectStruct buildArpg = ArpgUtility.hasObject(appearanceArpg, "build")
                ? appearanceArpg.getAsObject("build")
                : new ArpgObjectStruct();

        AppearanceData appearanceData = new AppearanceData(
                rigHandle,
                rigHandle.getBoneIndex(headBoneName),
                characterMesh,
                colors,
                parsePalettes(appearanceArpg, colors, sourceName),
                parseDetails(appearanceArpg, sourceName),
                parseShapes(appearanceArpg, characterMesh, sourceName),
                parseExpressions(appearanceArpg, characterMesh, sourceName),
                parseFeatures(appearanceArpg, sourceName),
                ArpgUtility.getFloat(buildArpg, "thin", EngineSetting.DEFAULT_BUILD_FACTOR),
                ArpgUtility.getFloat(buildArpg, "heavy", EngineSetting.DEFAULT_BUILD_FACTOR),
                parseGirthInfluence(buildArpg, rigHandle, sourceName));

        for (FeatureSlot featureSlot : FeatureSlot.VALUES) {

            FeatureHandle featureHandle = appearanceData.getDefaultFeature(featureSlot);

            if (featureHandle != null && !appearanceData.isCompatible(featureHandle))
                throwException("Default feature \"" + featureHandle.getFeatureName()
                        + "\" does not fit this character — its mesh must use the character's rig. Source: "
                        + sourceName);
        }

        return appearanceData;
    }

    // Colors \\

    private Color[] parseColors(ArpgObjectStruct appearanceArpg, String sourceName) {

        Color[] colors = new Color[SkinnedColor.VALUES.length];
        ArpgObjectStruct colorsArpg = ArpgUtility.hasObject(appearanceArpg, "colors")
                ? appearanceArpg.getAsObject("colors")
                : new ArpgObjectStruct();

        validateKeys(colorsArpg, SkinnedColor.class, "colors", sourceName);

        for (SkinnedColor skinnedColor : SkinnedColor.VALUES) {

            String key = ArpgUtility.toEnumName(skinnedColor);

            colors[skinnedColor.ordinal()] = ArpgUtility.hasArray(colorsArpg, key)
                    ? toColor(ArpgUtility.validateArray(colorsArpg, key, 3))
                    : new Color(Color.WHITE);
        }

        return colors;
    }

    private Color[][] parsePalettes(ArpgObjectStruct appearanceArpg, Color[] colors, String sourceName) {

        Color[][] palettes = new Color[SkinnedColor.VALUES.length][];
        ArpgObjectStruct palettesArpg = ArpgUtility.hasObject(appearanceArpg, "palettes")
                ? appearanceArpg.getAsObject("palettes")
                : new ArpgObjectStruct();

        validateKeys(palettesArpg, SkinnedColor.class, "palettes", sourceName);

        for (SkinnedColor skinnedColor : SkinnedColor.VALUES)
            palettes[skinnedColor.ordinal()] = parsePalette(
                    palettesArpg,
                    ArpgUtility.toEnumName(skinnedColor),
                    colors[skinnedColor.ordinal()],
                    sourceName);

        return palettes;
    }

    private Color[] parsePalette(ArpgObjectStruct palettesArpg, String key, Color defaultColor, String sourceName) {

        if (!ArpgUtility.hasArray(palettesArpg, key))
            return new Color[] { new Color(defaultColor) };

        ArpgArrayStruct paletteArpg = palettesArpg.getAsArray(key);

        if (paletteArpg.size() == 0)
            throwException("Appearance palette \"" + key + "\" must list at least one color. Source: " + sourceName);

        Color[] palette = new Color[paletteArpg.size()];

        for (int i = 0; i < palette.length; i++) {

            ArpgArrayStruct color = paletteArpg.get(i).getAsArray();

            if (color.size() != 3)
                throwException("Appearance palette \"" + key + "\" entry " + i + " must be [r, g, b]. Source: "
                        + sourceName);

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

    // Details \\

    private float[] parseDetails(ArpgObjectStruct appearanceArpg, String sourceName) {

        float[] details = new float[SkinnedDetail.VALUES.length];
        ArpgObjectStruct detailsArpg = ArpgUtility.hasObject(appearanceArpg, "details")
                ? appearanceArpg.getAsObject("details")
                : new ArpgObjectStruct();

        validateKeys(detailsArpg, SkinnedDetail.class, "details", sourceName);

        for (SkinnedDetail skinnedDetail : SkinnedDetail.VALUES)
            details[skinnedDetail.ordinal()] = ArpgUtility.getFloat(
                    detailsArpg,
                    ArpgUtility.toEnumName(skinnedDetail),
                    EngineSetting.APPEARANCE_DETAIL_DEFAULTS[skinnedDetail.ordinal()]);

        return details;
    }

    // Shapes \\

    private AppearanceShapeStruct[] parseShapes(
            ArpgObjectStruct appearanceArpg,
            MeshHandle characterMesh,
            String sourceName) {

        if (!ArpgUtility.hasArray(appearanceArpg, "shapes"))
            return new AppearanceShapeStruct[0];

        ArpgArrayStruct shapesArpg = appearanceArpg.getAsArray("shapes");
        AppearanceShapeStruct[] shapes = new AppearanceShapeStruct[shapesArpg.size()];

        for (int i = 0; i < shapes.length; i++) {

            ArpgObjectStruct shapeArpg = shapesArpg.get(i).getAsObject();
            String morphName = ArpgUtility.validateString(shapeArpg, "morph");
            float min = ArpgUtility.getFloat(shapeArpg, "min", EngineSetting.APPEARANCE_SHAPE_MIN);
            float max = ArpgUtility.getFloat(shapeArpg, "max", EngineSetting.APPEARANCE_SHAPE_MAX);
            float defaultValue = ArpgUtility.getFloat(shapeArpg, "default", EngineSetting.APPEARANCE_SHAPE_DEFAULT);

            if (max <= min || defaultValue < min || defaultValue > max)
                throwException("Appearance shape \"" + morphName + "\" must have min below max and its default "
                        + "between them. Source: " + sourceName);

            for (int j = 0; j < i; j++)
                if (shapes[j].getMorphName().equals(morphName))
                    throwException("Appearance shape \"" + morphName + "\" is listed twice. Source: " + sourceName);

            shapes[i] = new AppearanceShapeStruct(
                    morphName,
                    ArpgUtility.getString(shapeArpg, "group", EngineSetting.APPEARANCE_SHAPE_GROUP_DEFAULT),
                    resolveMorph(characterMesh, morphName, sourceName),
                    min,
                    max,
                    defaultValue);
        }

        return shapes;
    }

    // Expressions \\

    private int[] parseExpressions(ArpgObjectStruct appearanceArpg, MeshHandle characterMesh, String sourceName) {

        int[] expressionMorphs = new int[AppearanceExpression.VALUES.length];
        ArpgObjectStruct expressionsArpg = ArpgUtility.hasObject(appearanceArpg, "expressions")
                ? appearanceArpg.getAsObject("expressions")
                : new ArpgObjectStruct();

        validateKeys(expressionsArpg, AppearanceExpression.class, "expressions", sourceName);

        for (AppearanceExpression expression : AppearanceExpression.VALUES) {

            String key = ArpgUtility.toEnumName(expression);

            expressionMorphs[expression.ordinal()] = ArpgUtility.hasString(expressionsArpg, key)
                    ? resolveMorph(characterMesh, expressionsArpg.get(key).getAsString(), sourceName)
                    : EngineSetting.INDEX_NOT_FOUND;
        }

        return expressionMorphs;
    }

    private int resolveMorph(MeshHandle characterMesh, String morphName, String sourceName) {

        int morphIndex = characterMesh.getMorphIndex(morphName);

        if (morphIndex == EngineSetting.INDEX_NOT_FOUND)
            throwException("Appearance names morph \"" + morphName + "\" but the character mesh \""
                    + characterMesh.getMeshName() + "\" has no morph of that name. Source: " + sourceName);

        return morphIndex;
    }

    // Features \\

    private FeatureHandle[] parseFeatures(ArpgObjectStruct appearanceArpg, String sourceName) {

        FeatureHandle[] features = new FeatureHandle[FeatureSlot.VALUES.length];

        if (!ArpgUtility.hasObject(appearanceArpg, "features"))
            return features;

        ArpgObjectStruct featuresArpg = appearanceArpg.getAsObject("features");

        validateKeys(featuresArpg, FeatureSlot.class, "features", sourceName);

        for (FeatureSlot featureSlot : FeatureSlot.VALUES) {

            String key = ArpgUtility.toEnumName(featureSlot);

            if (!ArpgUtility.hasString(featuresArpg, key))
                continue;

            FeatureHandle featureHandle = featureManager.getFeatureHandleFromFeatureName(
                    featuresArpg.get(key).getAsString());

            if (featureHandle.getFeatureSlot() != featureSlot)
                throwException("Feature \"" + featureHandle.getFeatureName() + "\" is a "
                        + featureHandle.getFeatureSlot() + " feature but is declared under \"" + key
                        + "\". Source: " + sourceName);

            features[featureSlot.ordinal()] = featureHandle;
        }

        return features;
    }

    // Keys \\

    // Every key of a block keyed by an enum's names must name one of its constants
    private <E extends Enum<E>> void validateKeys(
            ArpgObjectStruct blockArpg,
            Class<E> enumClass,
            String blockName,
            String sourceName) {

        for (String key : blockArpg.keySet()) {

            boolean known = false;

            for (E constant : enumClass.getEnumConstants())
                known |= constant.name().equalsIgnoreCase(key);

            if (!known)
                throwException("Appearance \"" + blockName + "\" declares unknown key \"" + key + "\". Source: "
                        + sourceName);
        }
    }

    // Build Curve \\

    private float[] parseGirthInfluence(ArpgObjectStruct buildArpg, RigHandle rigHandle, String sourceName) {

        float[] girthInfluence = new float[rigHandle.getBoneCount()];

        if (!ArpgUtility.hasObject(buildArpg, "bones"))
            return girthInfluence;

        ArpgObjectStruct bonesArpg = buildArpg.getAsObject("bones");

        for (String boneName : bonesArpg.keySet()) {

            if (!rigHandle.hasBone(boneName))
                throwException("Appearance build references unknown bone \"" + boneName + "\". Source: "
                        + sourceName);

            girthInfluence[rigHandle.getBoneIndex(boneName)] = bonesArpg.get(boneName).getAsFloat();
        }

        return girthInfluence;
    }
}
