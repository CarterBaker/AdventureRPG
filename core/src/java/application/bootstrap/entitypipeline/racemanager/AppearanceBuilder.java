package application.bootstrap.entitypipeline.racemanager;

import application.bootstrap.entitypipeline.appearance.AppearanceBoneTargetStruct;
import application.bootstrap.entitypipeline.appearance.AppearanceData;
import application.bootstrap.entitypipeline.appearance.AppearanceExpression;
import application.bootstrap.entitypipeline.appearance.AppearanceGenderStruct;
import application.bootstrap.entitypipeline.appearance.AppearanceShapeStruct;
import application.bootstrap.entitypipeline.feature.FeatureHandle;
import application.bootstrap.entitypipeline.feature.FeatureSlot;
import application.bootstrap.entitypipeline.featuremanager.FeatureManager;
import application.bootstrap.entitypipeline.race.RaceGenderStruct;
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
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class AppearanceBuilder extends BuilderPackage {

    /*
     * Parses a race's "appearance" block into an AppearanceData, resolved
     * against the race's rig and the body mesh each of its genders wears.
     * "colors", "palettes" and "details" are keyed by the lower-case
     * SkinnedColor and SkinnedDetail names; "shapes" lists the sliders,
     * each known by its "name" (its morph's name when it gives none) and
     * driving a "morph", the "bones" it moves, turns and scales per unit of
     * its value, or both, with its group and range; "expressions" names the
     * morph each AppearanceExpression plays. The race's "genders" give each
     * gender its starting "shapes", by name, and "features", on the body it
     * wears; a feature named "" wears nothing in its slot, so a gender can
     * go bald under a race whose default gives hair. Every body must carry
     * every morph the sliders and expressions name. Every default feature
     * must sit in the slot it is declared under and fit the rig. A color
     * left out is white, a palette left out holds only its default color, a
     * detail left out is EngineSetting's default, and the build curve
     * defaults to a flat 1.0. Bootstrap-only, apart from an editor's
     * preview builds.
     */

    // Internal
    private FeatureManager featureManager;

    // Base \\

    @Override
    protected void get() {
        this.featureManager = get(FeatureManager.class);
    }

    // Build \\

    AppearanceData build(
            ArpgObjectStruct appearanceArpg,
            ArpgObjectStruct gendersArpg,
            RaceGenderStruct[] genders,
            String sourceName) {

        RigHandle rigHandle = genders[0].getRigHandle();
        String headBoneName = ArpgUtility.validateString(appearanceArpg, "head_bone");

        if (!rigHandle.hasBone(headBoneName))
            throwException("Appearance \"head_bone\" \"" + headBoneName
                    + "\" is not a bone of the character rig. Source: " + sourceName);

        Color[] colors = parseColors(appearanceArpg, sourceName);
        ArpgObjectStruct buildArpg = ArpgUtility.hasObject(appearanceArpg, "build")
                ? appearanceArpg.getAsObject("build")
                : new ArpgObjectStruct();

        MeshHandle[] bodyMeshes = parseBodies(genders);
        AppearanceShapeStruct[] shapes = parseShapes(appearanceArpg, rigHandle, sourceName);
        AppearanceData appearanceData = new AppearanceData(
                rigHandle,
                rigHandle.getBoneIndex(headBoneName),
                bodyMeshes,
                colors,
                parsePalettes(appearanceArpg, colors, sourceName),
                parseDetails(appearanceArpg, sourceName),
                shapes,
                resolveShapeMorphs(bodyMeshes, shapes, sourceName),
                parseExpressions(appearanceArpg, bodyMeshes, sourceName),
                parseFeatures(appearanceArpg, sourceName),
                parseGenders(gendersArpg, genders, bodyMeshes, shapes, sourceName),
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

    // Bodies \\

    // Every mesh a gender wears, each once, in the order the genders are listed
    private MeshHandle[] parseBodies(RaceGenderStruct[] genders) {

        ObjectArrayList<MeshHandle> bodies = new ObjectArrayList<>();

        for (int i = 0; i < genders.length; i++)
            if (!bodies.contains(genders[i].getMeshHandle()))
                bodies.add(genders[i].getMeshHandle());

        return bodies.toArray(new MeshHandle[0]);
    }

    // Shapes \\

    private AppearanceShapeStruct[] parseShapes(
            ArpgObjectStruct appearanceArpg,
            RigHandle rigHandle,
            String sourceName) {

        if (!ArpgUtility.hasArray(appearanceArpg, "shapes"))
            return new AppearanceShapeStruct[0];

        ArpgArrayStruct shapesArpg = appearanceArpg.getAsArray("shapes");
        AppearanceShapeStruct[] shapes = new AppearanceShapeStruct[shapesArpg.size()];

        for (int i = 0; i < shapes.length; i++) {

            ArpgObjectStruct shapeArpg = shapesArpg.get(i).getAsObject();
            String morphName = ArpgUtility.hasString(shapeArpg, "morph") ? shapeArpg.get("morph").getAsString() : null;
            String shapeName = ArpgUtility.getString(shapeArpg, "name", morphName);
            float min = ArpgUtility.getFloat(shapeArpg, "min", EngineSetting.APPEARANCE_SHAPE_MIN);
            float max = ArpgUtility.getFloat(shapeArpg, "max", EngineSetting.APPEARANCE_SHAPE_MAX);
            float defaultValue = ArpgUtility.getFloat(shapeArpg, "default", EngineSetting.APPEARANCE_SHAPE_DEFAULT);

            if (shapeName == null || shapeName.isEmpty())
                throwException("Appearance shape " + i + " has neither a \"name\" nor a \"morph\" to be known by. "
                        + "Source: " + sourceName);

            if (max <= min || defaultValue < min || defaultValue > max)
                throwException("Appearance shape \"" + shapeName + "\" must have min below max and its default "
                        + "between them. Source: " + sourceName);

            for (int j = 0; j < i; j++)
                if (shapes[j].getShapeName().equals(shapeName))
                    throwException("Appearance shape \"" + shapeName + "\" is listed twice. Source: " + sourceName);

            AppearanceBoneTargetStruct[] boneTargets = parseBoneTargets(shapeArpg, rigHandle, shapeName, sourceName);

            if (morphName == null && boneTargets.length == 0)
                throwException("Appearance shape \"" + shapeName + "\" drives nothing — give it a \"morph\" or "
                        + "\"bones\". Source: " + sourceName);

            shapes[i] = new AppearanceShapeStruct(
                    shapeName,
                    ArpgUtility.getString(shapeArpg, "group", EngineSetting.APPEARANCE_SHAPE_GROUP_DEFAULT),
                    morphName,
                    boneTargets,
                    min,
                    max,
                    defaultValue);
        }

        return shapes;
    }

    // "bones" lists what one unit of the slider does to each bone it drives, every bone at most once
    private AppearanceBoneTargetStruct[] parseBoneTargets(
            ArpgObjectStruct shapeArpg,
            RigHandle rigHandle,
            String shapeName,
            String sourceName) {

        if (!ArpgUtility.hasArray(shapeArpg, "bones"))
            return new AppearanceBoneTargetStruct[0];

        ArpgArrayStruct bonesArpg = shapeArpg.getAsArray("bones");
        AppearanceBoneTargetStruct[] targets = new AppearanceBoneTargetStruct[bonesArpg.size()];

        for (int i = 0; i < targets.length; i++) {

            ArpgObjectStruct targetArpg = bonesArpg.get(i).getAsObject();
            String boneName = ArpgUtility.validateString(targetArpg, "bone");

            if (!rigHandle.hasBone(boneName))
                throwException("Appearance shape \"" + shapeName + "\" drives unknown bone \"" + boneName
                        + "\". Source: " + sourceName);

            int boneIndex = rigHandle.getBoneIndex(boneName);

            for (int j = 0; j < i; j++)
                if (targets[j].getBoneIndex() == boneIndex)
                    throwException("Appearance shape \"" + shapeName + "\" drives bone \"" + boneName
                            + "\" twice. Source: " + sourceName);

            targets[i] = new AppearanceBoneTargetStruct(
                    boneIndex,
                    parseAmount(targetArpg, "move"),
                    parseAmount(targetArpg, "turn"),
                    parseAmount(targetArpg, "scale"));
        }

        return targets;
    }

    private Vector3 parseAmount(ArpgObjectStruct targetArpg, String key) {

        if (!ArpgUtility.hasArray(targetArpg, key))
            return new Vector3();

        ArpgArrayStruct amount = ArpgUtility.validateArray(targetArpg, key, 3);

        return new Vector3(amount.get(0).getAsFloat(), amount.get(1).getAsFloat(), amount.get(2).getAsFloat());
    }

    // The morph each slider weighs on every body, INDEX_NOT_FOUND for a slider that drives only bones
    private int[][] resolveShapeMorphs(MeshHandle[] bodyMeshes, AppearanceShapeStruct[] shapes, String sourceName) {

        int[][] shapeMorphs = new int[bodyMeshes.length][shapes.length];

        for (int b = 0; b < bodyMeshes.length; b++)
            for (int i = 0; i < shapes.length; i++)
                shapeMorphs[b][i] = shapes[i].hasMorph()
                        ? resolveMorph(bodyMeshes[b], shapes[i].getMorphName(), sourceName)
                        : EngineSetting.INDEX_NOT_FOUND;

        return shapeMorphs;
    }

    // Expressions \\

    private int[][] parseExpressions(ArpgObjectStruct appearanceArpg, MeshHandle[] bodyMeshes, String sourceName) {

        int[][] expressionMorphs = new int[bodyMeshes.length][AppearanceExpression.VALUES.length];
        ArpgObjectStruct expressionsArpg = ArpgUtility.hasObject(appearanceArpg, "expressions")
                ? appearanceArpg.getAsObject("expressions")
                : new ArpgObjectStruct();

        validateKeys(expressionsArpg, AppearanceExpression.class, "expressions", sourceName);

        for (int b = 0; b < bodyMeshes.length; b++)
            for (AppearanceExpression expression : AppearanceExpression.VALUES) {

                String key = ArpgUtility.toEnumName(expression);

                expressionMorphs[b][expression.ordinal()] = ArpgUtility.hasString(expressionsArpg, key)
                        ? resolveMorph(bodyMeshes[b], expressionsArpg.get(key).getAsString(), sourceName)
                        : EngineSetting.INDEX_NOT_FOUND;
            }

        return expressionMorphs;
    }

    private int resolveMorph(MeshHandle bodyMesh, String morphName, String sourceName) {

        int morphIndex = bodyMesh.getMorphIndex(morphName);

        if (morphIndex == EngineSetting.INDEX_NOT_FOUND)
            throwException("Appearance names morph \"" + morphName + "\" but the body mesh \""
                    + bodyMesh.getMeshName() + "\" has no morph of that name — every body must carry every morph "
                    + "the sliders and expressions name. Source: " + sourceName);

        return morphIndex;
    }

    // Genders \\

    // Each gender's starting look: the body it wears and the "shapes" (by name) and "features" it starts with
    private Int2ObjectOpenHashMap<AppearanceGenderStruct> parseGenders(
            ArpgObjectStruct gendersArpg,
            RaceGenderStruct[] genders,
            MeshHandle[] bodyMeshes,
            AppearanceShapeStruct[] shapes,
            String sourceName) {

        Int2ObjectOpenHashMap<AppearanceGenderStruct> genderID2Preset = new Int2ObjectOpenHashMap<>();
        int index = 0;

        for (String genderName : gendersArpg.keySet()) {

            ArpgObjectStruct presetArpg = gendersArpg.getAsObject(genderName);
            RaceGenderStruct gender = genders[index++];
            float[] shapeValues = new float[shapes.length];
            ArpgObjectStruct shapesArpg = ArpgUtility.hasObject(presetArpg, "shapes")
                    ? presetArpg.getAsObject("shapes")
                    : new ArpgObjectStruct();

            for (int i = 0; i < shapes.length; i++)
                shapeValues[i] = ArpgUtility.getFloat(shapesArpg, shapes[i].getShapeName(), Float.NaN);

            for (String shapeName : shapesArpg.keySet())
                if (!hasShape(shapes, shapeName))
                    throwException("Race gender \"" + genderName + "\" sets \"" + shapeName
                            + "\", which is not a shape slider. Source: " + sourceName);

            genderID2Preset.put(
                    gender.getGenderID(),
                    new AppearanceGenderStruct(
                            resolveBodyIndex(gender.getMeshHandle(), bodyMeshes),
                            shapeValues,
                            parseFeatures(presetArpg, sourceName),
                            parseNamedFeatures(presetArpg)));
        }

        return genderID2Preset;
    }

    // The body a gender wears among the bodies parsed
    private int resolveBodyIndex(MeshHandle bodyMesh, MeshHandle[] bodyMeshes) {

        for (int i = 0; i < bodyMeshes.length; i++)
            if (bodyMeshes[i] == bodyMesh)
                return i;

        return 0;
    }

    private boolean hasShape(AppearanceShapeStruct[] shapes, String shapeName) {

        for (AppearanceShapeStruct shape : shapes)
            if (shape.getShapeName().equals(shapeName))
                return true;

        return false;
    }

    // Features \\

    private FeatureHandle[] parseFeatures(ArpgObjectStruct arpg, String sourceName) {

        FeatureHandle[] features = new FeatureHandle[FeatureSlot.VALUES.length];

        if (!ArpgUtility.hasObject(arpg, "features"))
            return features;

        ArpgObjectStruct featuresArpg = arpg.getAsObject("features");

        validateKeys(featuresArpg, FeatureSlot.class, "features", sourceName);

        for (FeatureSlot featureSlot : FeatureSlot.VALUES) {

            String key = ArpgUtility.toEnumName(featureSlot);

            if (!ArpgUtility.hasString(featuresArpg, key))
                continue;

            String featureName = featuresArpg.get(key).getAsString();

            if (featureName.equals(EngineSetting.APPEARANCE_FEATURE_NONE))
                continue;

            FeatureHandle featureHandle = featureManager.getFeatureHandleFromFeatureName(featureName);

            if (featureHandle.getFeatureSlot() != featureSlot)
                throwException("Feature \"" + featureHandle.getFeatureName() + "\" is a "
                        + featureHandle.getFeatureSlot() + " feature but is declared under \"" + key
                        + "\". Source: " + sourceName);

            features[featureSlot.ordinal()] = featureHandle;
        }

        return features;
    }

    // Which slots a look names for itself, a feature or none
    private boolean[] parseNamedFeatures(ArpgObjectStruct arpg) {

        boolean[] namedFeatures = new boolean[FeatureSlot.VALUES.length];

        if (!ArpgUtility.hasObject(arpg, "features"))
            return namedFeatures;

        ArpgObjectStruct featuresArpg = arpg.getAsObject("features");

        for (FeatureSlot featureSlot : FeatureSlot.VALUES)
            namedFeatures[featureSlot.ordinal()] = ArpgUtility.hasString(
                    featuresArpg,
                    ArpgUtility.toEnumName(featureSlot));

        return namedFeatures;
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
