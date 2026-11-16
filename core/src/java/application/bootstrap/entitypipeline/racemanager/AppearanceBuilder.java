package application.bootstrap.entitypipeline.racemanager;

import java.util.Arrays;

import application.bootstrap.entitypipeline.appearance.AppearanceBoneTargetStruct;
import application.bootstrap.entitypipeline.appearance.AppearanceData;
import application.bootstrap.entitypipeline.appearance.AppearanceDriveStruct;
import application.bootstrap.animationpipeline.animation.AnimationClipHandle;
import application.bootstrap.animationpipeline.animationmanager.AnimationManager;
import application.bootstrap.entitypipeline.appearance.AppearanceExpressionStruct;
import application.bootstrap.entitypipeline.appearance.AppearanceGenderStruct;
import application.bootstrap.entitypipeline.appearance.AppearanceOptionKind;
import application.bootstrap.entitypipeline.appearance.AppearanceOptionStruct;
import application.bootstrap.entitypipeline.appearance.AppearanceSectionStruct;
import application.bootstrap.entitypipeline.appearance.AppearanceShapeStruct;
import application.bootstrap.entitypipeline.appearance.AppearanceTabStruct;
import application.bootstrap.entitypipeline.bonecontrol.BoneControlDriveStruct;
import application.bootstrap.entitypipeline.bonecontrol.BoneControlHandle;
import application.bootstrap.entitypipeline.bonecontrol.BoneControlStruct;
import application.bootstrap.entitypipeline.bonecontrolmanager.BoneControlManager;
import application.bootstrap.entitypipeline.feature.FeatureHandle;
import application.bootstrap.entitypipeline.feature.FeatureSlot;
import application.bootstrap.entitypipeline.featuremanager.FeatureManager;
import application.bootstrap.entitypipeline.race.RaceGenderStruct;
import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.rig.BoneSliderChannel;
import application.bootstrap.geometrypipeline.rig.RigBoneStruct;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedColor;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedDetail;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedOverlay;
import engine.graphics.color.Color;
import engine.root.BuilderPackage;
import engine.root.EngineSetting;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgElementStruct;
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
     * SkinnedColor and SkinnedDetail names. The sliders come from the
     * character itself: a bone slider for each channel the rig's bones
     * offer, a control for each of the rig's controls (BoneControlManager)
     * and a shape key for each morph of the bodies that no expression plays;
     * "sliders" gives any of them, by name, its "group", "min", "max",
     * "default" and whether the "player" is offered it, and lists them in
     * the order a creator shows them. "expressions" names the clip each of
     * the face's expressions plays, made for the race's rig, its bones the
     * face bones it moves and its morph curves the morphs it weighs, the one
     * named blink played by itself. The race's "genders" give each
     * gender its starting "shapes", by name, and "features", on the body it
     * wears; a feature named "" wears nothing in its slot, so a gender can
     * go bald under a race whose default gives hair. Every body must carry
     * every morph the sliders and expressions name. Every default feature
     * must sit in the slot it is declared under and fit the rig. A color
     * left out is white, a palette left out holds only its default color, a
     * detail left out is EngineSetting's default, and the build curve
     * defaults to a flat 1.0. "player" flags each color, detail, overlay and
     * feature the player is offered, every one offered unless it says
     * otherwise, and "creator" lays out the game's creator: its tabs, each
     * with its sections, each listing options by "kind" and "name"; an
     * offered option or slider group the layout leaves out gains a section
     * of its kind in the last tab, so nothing the player is offered is lost,
     * and a race without "creator" is shown a Body tab (race, gender, build,
     * head shape) and a Look tab holding the rest. Bootstrap-only, apart from an editor's
     * preview builds.
     */

    // Internal
    private FeatureManager featureManager;
    private BoneControlManager boneControlManager;
    private AnimationManager animationManager;

    // Base \\

    @Override
    protected void get() {
        this.featureManager = get(FeatureManager.class);
        this.boneControlManager = get(BoneControlManager.class);
        this.animationManager = get(AnimationManager.class);
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
        AppearanceExpressionStruct[] expressions = parseExpressions(appearanceArpg, rigHandle, bodyMeshes, sourceName);
        AppearanceShapeStruct[] shapes = parseShapes(appearanceArpg, rigHandle, bodyMeshes, expressions, sourceName);
        FeatureHandle[] defaultFeatures = parseFeatures(appearanceArpg, sourceName);
        boolean[][] offered = parseOffered(appearanceArpg, sourceName);
        AppearanceData appearanceData = new AppearanceData(
                rigHandle,
                rigHandle.getBoneIndex(headBoneName),
                bodyMeshes,
                colors,
                parsePalettes(appearanceArpg, colors, sourceName),
                parseDetails(appearanceArpg, sourceName),
                shapes,
                resolveShapeMorphs(bodyMeshes, shapes, sourceName),
                expressions,
                defaultFeatures,
                parseGenders(gendersArpg, genders, bodyMeshes, shapes, defaultFeatures, sourceName),
                ArpgUtility.getFloat(buildArpg, "thin", EngineSetting.DEFAULT_BUILD_FACTOR),
                ArpgUtility.getFloat(buildArpg, "heavy", EngineSetting.DEFAULT_BUILD_FACTOR),
                parseGirthInfluence(buildArpg, rigHandle, sourceName),
                parseCreator(appearanceArpg, shapes, offered, sourceName),
                offered);

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

    // Every slider the race offers: a bone slider per channel its rig's bones offer (a left and right pair as one),
    // a control per control of the rig, and a shape key per morph of the bodies that no expression plays; those the
    // race's "sliders" name first, in its order, then the rest in that order, each taking its "group", range and
    // "player" from the race where it names them
    private AppearanceShapeStruct[] parseShapes(
            ArpgObjectStruct appearanceArpg,
            RigHandle rigHandle,
            MeshHandle[] bodyMeshes,
            AppearanceExpressionStruct[] expressions,
            String sourceName) {

        ArpgObjectStruct slidersArpg = ArpgUtility.hasObject(appearanceArpg, "sliders")
                ? appearanceArpg.getAsObject("sliders")
                : new ArpgObjectStruct();
        ObjectArrayList<String> names = new ObjectArrayList<>();
        ObjectArrayList<AppearanceBoneTargetStruct[]> targets = new ObjectArrayList<>();
        ObjectArrayList<BoneControlStruct> controls = new ObjectArrayList<>();
        ObjectArrayList<String> morphs = new ObjectArrayList<>();

        collectBoneSliders(rigHandle, names, targets, controls, morphs);
        collectControls(rigHandle, names, targets, controls, morphs);
        collectShapeKeys(expressions, bodyMeshes, names, targets, controls, morphs);

        for (String key : slidersArpg.keySet())
            if (!names.contains(key))
                throwException("Appearance \"sliders\" names \"" + key + "\", which is no bone slider, control or "
                        + "shape key of this character. Source: " + sourceName);

        int[] order = resolveOrder(slidersArpg, names);
        AppearanceShapeStruct[] shapes = new AppearanceShapeStruct[order.length];

        for (int i = 0; i < order.length; i++)
            shapes[i] = buildShape(
                    slidersArpg,
                    names.get(order[i]),
                    targets.get(order[i]),
                    controls.get(order[i]),
                    morphs.get(order[i]),
                    sourceName);

        return resolveDrives(shapes, controls, order, sourceName);
    }

    // A slider per channel each bone offers, named "<bone>/<channel>"; a bone whose twin across the body offers the
    // same channel shares its slider under the name they share, the right side taking the channel's mirror
    private void collectBoneSliders(
            RigHandle rigHandle,
            ObjectArrayList<String> names,
            ObjectArrayList<AppearanceBoneTargetStruct[]> targets,
            ObjectArrayList<BoneControlStruct> controls,
            ObjectArrayList<String> morphs) {

        for (int b = 0; b < rigHandle.getBoneCount(); b++) {

            RigBoneStruct bone = rigHandle.getBone(b);
            String boneName = bone.getName();
            int twin = findTwin(rigHandle, boneName);
            boolean right = boneName.endsWith(EngineSetting.BONE_SIDE_SUFFIX_RIGHT);

            for (BoneSliderChannel channel : BoneSliderChannel.VALUES) {

                if (!bone.offersSlider(channel))
                    continue;

                boolean paired = twin != EngineSetting.INDEX_NOT_FOUND && rigHandle.getBone(twin).offersSlider(channel);

                if (paired && right)
                    continue;

                String baseName = paired ? boneName.substring(0, boneName.length()
                        - EngineSetting.BONE_SIDE_SUFFIX_LEFT.length()) : boneName;
                AppearanceBoneTargetStruct[] boneTargets = paired
                        ? new AppearanceBoneTargetStruct[] {
                                toChannelTarget(b, channel, 1f),
                                toChannelTarget(twin, channel, channel.getMirrorSign()) }
                        : new AppearanceBoneTargetStruct[] { toChannelTarget(b, channel, 1f) };

                addCandidate(names, targets, controls, morphs,
                        baseName + EngineSetting.BONE_SLIDER_NAME_SEPARATOR + ArpgUtility.toEnumName(channel),
                        boneTargets, null, null);
            }
        }
    }

    // The right twin of a left bone, or the left of a right one; INDEX_NOT_FOUND for a bone with no side or no twin
    private int findTwin(RigHandle rigHandle, String boneName) {

        String left = EngineSetting.BONE_SIDE_SUFFIX_LEFT;
        String right = EngineSetting.BONE_SIDE_SUFFIX_RIGHT;
        String twinName = boneName.endsWith(left)
                ? boneName.substring(0, boneName.length() - left.length()) + right
                : boneName.endsWith(right) ? boneName.substring(0, boneName.length() - right.length()) + left : null;

        return twinName != null && rigHandle.hasBone(twinName)
                ? rigHandle.getBoneIndex(twinName)
                : EngineSetting.INDEX_NOT_FOUND;
    }

    // One unit of a channel on one bone: a block moved, a degree turned or the whole of its size added on the axis
    private AppearanceBoneTargetStruct toChannelTarget(int boneIndex, BoneSliderChannel channel, float sign) {

        int axis = channel.getAxis();
        Vector3 unit = new Vector3(axis == 0 ? sign : 0f, axis == 1 ? sign : 0f, axis == 2 ? sign : 0f);
        int kind = channel.getKind();

        return new AppearanceBoneTargetStruct(
                boneIndex,
                kind == BoneSliderChannel.KIND_MOVE ? unit : new Vector3(),
                kind == BoneSliderChannel.KIND_TURN ? unit : new Vector3(),
                kind == BoneSliderChannel.KIND_SIZE ? unit : new Vector3());
    }

    private void collectControls(
            RigHandle rigHandle,
            ObjectArrayList<String> names,
            ObjectArrayList<AppearanceBoneTargetStruct[]> targets,
            ObjectArrayList<BoneControlStruct> controls,
            ObjectArrayList<String> morphs) {

        BoneControlHandle handle = boneControlManager.getBoneControlHandle(rigHandle.getRigName());

        if (handle == null)
            return;

        for (int c = 0; c < handle.getControlCount(); c++)
            addCandidate(names, targets, controls, morphs, handle.getControl(c).getControlName(),
                    new AppearanceBoneTargetStruct[0], handle.getControl(c), null);
    }

    // Every morph of the first body no expression plays; every body carries the same morphs
    private void collectShapeKeys(
            AppearanceExpressionStruct[] expressions,
            MeshHandle[] bodyMeshes,
            ObjectArrayList<String> names,
            ObjectArrayList<AppearanceBoneTargetStruct[]> targets,
            ObjectArrayList<BoneControlStruct> controls,
            ObjectArrayList<String> morphs) {

        MeshHandle body = bodyMeshes[0];

        for (int m = 0; m < body.getMorphCount(); m++) {

            String morphName = body.getMorphName(m);

            if (!playsExpression(expressions, morphName))
                addCandidate(names, targets, controls, morphs, morphName, new AppearanceBoneTargetStruct[0], null,
                        morphName);
        }
    }

    private boolean playsExpression(AppearanceExpressionStruct[] expressions, String morphName) {

        for (int i = 0; i < expressions.length; i++)
            if (expressions[i].playsMorph(morphName))
                return true;

        return false;
    }

    private void addCandidate(
            ObjectArrayList<String> names,
            ObjectArrayList<AppearanceBoneTargetStruct[]> targets,
            ObjectArrayList<BoneControlStruct> controls,
            ObjectArrayList<String> morphs,
            String name,
            AppearanceBoneTargetStruct[] boneTargets,
            BoneControlStruct control,
            String morphName) {

        if (names.contains(name))
            throwException("Appearance slider \"" + name + "\" is offered twice: a control or shape key shares its "
                    + "name with another slider");

        names.add(name);
        targets.add(boneTargets);
        controls.add(control);
        morphs.add(morphName);
    }

    // The sliders the race names, in its order, then every other in the order they were found
    private int[] resolveOrder(ArpgObjectStruct slidersArpg, ObjectArrayList<String> names) {

        int[] order = new int[names.size()];
        boolean[] placed = new boolean[names.size()];
        int count = 0;

        for (String key : slidersArpg.keySet()) {

            int index = names.indexOf(key);
            order[count++] = index;
            placed[index] = true;
        }

        for (int i = 0; i < names.size(); i++)
            if (!placed[i])
                order[count++] = i;

        return order;
    }

    // A slider with the group, range and player choice the race gives it, else its own defaults: a bone slider spans
    // its channel's default range either way of zero, a control and a shape key the appearance defaults
    private AppearanceShapeStruct buildShape(
            ArpgObjectStruct slidersArpg,
            String name,
            AppearanceBoneTargetStruct[] boneTargets,
            BoneControlStruct control,
            String morphName,
            String sourceName) {

        ArpgObjectStruct sliderArpg = ArpgUtility.hasObject(slidersArpg, name)
                ? slidersArpg.getAsObject(name)
                : new ArpgObjectStruct();
        float range = boneTargets.length > 0 ? resolveChannelRange(name) : 0f;
        float min = ArpgUtility.getFloat(sliderArpg, "min",
                boneTargets.length > 0 ? -range : EngineSetting.APPEARANCE_SHAPE_MIN);
        float max = ArpgUtility.getFloat(sliderArpg, "max",
                boneTargets.length > 0 ? range : EngineSetting.APPEARANCE_SHAPE_MAX);
        float defaultValue = ArpgUtility.getFloat(sliderArpg, "default", EngineSetting.APPEARANCE_SHAPE_DEFAULT);
        String defaultGroup = control != null ? control.getGroupName() : EngineSetting.APPEARANCE_SHAPE_GROUP_DEFAULT;

        if (max <= min || defaultValue < min || defaultValue > max)
            throwException("Appearance slider \"" + name + "\" must have min below max and its default between them. "
                    + "Source: " + sourceName);

        return new AppearanceShapeStruct(
                name,
                ArpgUtility.getString(sliderArpg, "group", defaultGroup),
                morphName,
                boneTargets,
                new AppearanceDriveStruct[0],
                min,
                max,
                defaultValue,
                ArpgUtility.getBoolean(sliderArpg, "player", true));
    }

    // The default range of a bone slider, read from the channel its name ends in
    private float resolveChannelRange(String name) {

        String channelName = name.substring(name.lastIndexOf(EngineSetting.BONE_SLIDER_NAME_SEPARATOR)
                + EngineSetting.BONE_SLIDER_NAME_SEPARATOR.length());

        return ArpgUtility.toEnum(channelName, BoneSliderChannel.class).getDefaultRange();
    }

    // Every control given the bone sliders it drives, each found among the race's sliders by name
    private AppearanceShapeStruct[] resolveDrives(
            AppearanceShapeStruct[] shapes,
            ObjectArrayList<BoneControlStruct> controls,
            int[] order,
            String sourceName) {

        for (int i = 0; i < shapes.length; i++) {

            BoneControlStruct control = controls.get(order[i]);

            if (control == null)
                continue;

            AppearanceDriveStruct[] drives = new AppearanceDriveStruct[control.getDriveCount()];

            for (int d = 0; d < drives.length; d++) {

                BoneControlDriveStruct drive = control.getDrive(d);
                int sliderIndex = findShapeIndex(shapes, drive.getSliderName());

                if (sliderIndex == EngineSetting.INDEX_NOT_FOUND || !shapes[sliderIndex].hasBoneTargets())
                    throwException("Control \"" + control.getControlName() + "\" drives \"" + drive.getSliderName()
                            + "\", which is no bone slider of this character's rig. Source: " + sourceName);

                drives[d] = new AppearanceDriveStruct(sliderIndex, drive.isIncrease(), drive.getKeys());
            }

            AppearanceShapeStruct shape = shapes[i];

            shapes[i] = new AppearanceShapeStruct(
                    shape.getShapeName(),
                    shape.getGroupName(),
                    shape.getMorphName(),
                    new AppearanceBoneTargetStruct[0],
                    drives,
                    shape.getMin(),
                    shape.getMax(),
                    shape.getDefaultValue(),
                    shape.isShownToPlayer());
        }

        return shapes;
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

    // Every expression the race names, its clip made for the race's rig and each of the clip's morph curves resolved
    // on every body
    private AppearanceExpressionStruct[] parseExpressions(
            ArpgObjectStruct appearanceArpg,
            RigHandle rigHandle,
            MeshHandle[] bodyMeshes,
            String sourceName) {

        String key = EngineSetting.APPEARANCE_FIELD_EXPRESSIONS;
        ArpgObjectStruct expressionsArpg = ArpgUtility.hasObject(appearanceArpg, key)
                ? appearanceArpg.getAsObject(key)
                : new ArpgObjectStruct();
        AppearanceExpressionStruct[] expressions = new AppearanceExpressionStruct[expressionsArpg.size()];
        int index = 0;

        for (String expressionName : expressionsArpg.keySet()) {

            String clipName = ArpgUtility.validateString(expressionsArpg, expressionName);
            AnimationClipHandle clip = animationManager.getClipHandleFromClipName(clipName);

            if (clip.getRigHandle() != rigHandle)
                throwException("Appearance expression \"" + expressionName + "\" plays clip \"" + clipName
                        + "\", which is made for another rig than the race's. Source: " + sourceName);

            int[][] trackMorphs = new int[bodyMeshes.length][clip.getMorphTrackCount()];

            for (int b = 0; b < bodyMeshes.length; b++)
                for (int t = 0; t < clip.getMorphTrackCount(); t++)
                    trackMorphs[b][t] = resolveMorph(bodyMeshes[b], clip.getMorphTrack(t).getMorphName(), sourceName);

            expressions[index++] = new AppearanceExpressionStruct(expressionName, clip, trackMorphs);
        }

        return expressions;
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

    // Each gender's starting look: the body it wears, the "shapes" (by name) and "features" it starts with, the variant
    // of each feature it starts in ("feature_variants", by slot), and the "ranges" (by name, each a "min" below its
    // "max", within the slider's own) the player may slide its sliders across
    private Int2ObjectOpenHashMap<AppearanceGenderStruct> parseGenders(
            ArpgObjectStruct gendersArpg,
            RaceGenderStruct[] genders,
            MeshHandle[] bodyMeshes,
            AppearanceShapeStruct[] shapes,
            FeatureHandle[] defaultFeatures,
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

            float[] shapeMins = new float[shapes.length];
            float[] shapeMaxes = new float[shapes.length];

            FeatureHandle[] features = parseFeatures(presetArpg, sourceName);
            boolean[] namedFeatures = parseNamedFeatures(presetArpg);

            parseRanges(presetArpg, genderName, shapes, shapeMins, shapeMaxes, sourceName);
            genderID2Preset.put(
                    gender.getGenderID(),
                    new AppearanceGenderStruct(
                            resolveBodyIndex(gender.getMeshHandle(), bodyMeshes),
                            shapeValues,
                            shapeMins,
                            shapeMaxes,
                            features,
                            namedFeatures,
                            parseFeatureVariants(
                                    presetArpg, features, namedFeatures, defaultFeatures, genderName, sourceName)));
        }

        return genderID2Preset;
    }

    // A gender's own range for each slider it names in "ranges", NaN ends for every other
    private void parseRanges(
            ArpgObjectStruct presetArpg,
            String genderName,
            AppearanceShapeStruct[] shapes,
            float[] shapeMins,
            float[] shapeMaxes,
            String sourceName) {

        Arrays.fill(shapeMins, Float.NaN);
        Arrays.fill(shapeMaxes, Float.NaN);

        if (!ArpgUtility.hasObject(presetArpg, "ranges"))
            return;

        ArpgObjectStruct rangesArpg = presetArpg.getAsObject("ranges");

        for (String shapeName : rangesArpg.keySet()) {

            int shapeIndex = findShapeIndex(shapes, shapeName);
            ArpgObjectStruct rangeArpg = rangesArpg.getAsObject(shapeName);

            if (shapeIndex == EngineSetting.INDEX_NOT_FOUND)
                throwException("Race gender \"" + genderName + "\" ranges \"" + shapeName
                        + "\", which is not a shape slider. Source: " + sourceName);

            AppearanceShapeStruct shape = shapes[shapeIndex];
            float min = ArpgUtility.validateFloat(rangeArpg, "min");
            float max = ArpgUtility.validateFloat(rangeArpg, "max");

            if (min >= max || min < shape.getMin() || max > shape.getMax())
                throwException("Race gender \"" + genderName + "\" ranges \"" + shapeName + "\" from " + min + " to "
                        + max + ", which must rise within the slider's own " + shape.getMin() + " to "
                        + shape.getMax() + ". Source: " + sourceName);

            shapeMins[shapeIndex] = min;
            shapeMaxes[shapeIndex] = max;
        }
    }

    private int findShapeIndex(AppearanceShapeStruct[] shapes, String shapeName) {

        for (int i = 0; i < shapes.length; i++)
            if (shapes[i].getShapeName().equals(shapeName))
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
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

    // The variant a gender starts each worn feature in, by its place among the variants the feature it starts with
    // offers; INDEX_NOT_FOUND keeps the feature's own default
    private int[] parseFeatureVariants(
            ArpgObjectStruct presetArpg,
            FeatureHandle[] features,
            boolean[] namedFeatures,
            FeatureHandle[] defaultFeatures,
            String genderName,
            String sourceName) {

        int[] featureVariants = new int[FeatureSlot.VALUES.length];

        Arrays.fill(featureVariants, EngineSetting.INDEX_NOT_FOUND);

        if (!ArpgUtility.hasObject(presetArpg, "feature_variants"))
            return featureVariants;

        ArpgObjectStruct variantsArpg = presetArpg.getAsObject("feature_variants");

        validateKeys(variantsArpg, FeatureSlot.class, "feature_variants", sourceName);

        for (FeatureSlot featureSlot : FeatureSlot.VALUES) {

            String key = ArpgUtility.toEnumName(featureSlot);

            if (!ArpgUtility.hasString(variantsArpg, key))
                continue;

            int slotIndex = featureSlot.ordinal();
            FeatureHandle featureHandle = namedFeatures[slotIndex] ? features[slotIndex] : defaultFeatures[slotIndex];
            String variantName = variantsArpg.get(key).getAsString();
            int variantIndex = featureHandle != null && featureHandle.hasVariants()
                    ? featureHandle.getVariants().find(variantName)
                    : EngineSetting.INDEX_NOT_FOUND;

            if (variantIndex == EngineSetting.INDEX_NOT_FOUND)
                throwException("Race gender \"" + genderName + "\" starts its " + key + " in variant \""
                        + variantName + "\", which the " + key + " it wears does not offer. Source: " + sourceName);

            featureVariants[slotIndex] = variantIndex;
        }

        return featureVariants;
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
    // Creator \\

    // Whether the player is offered each member of every flagged kind, by kind then member ordinal
    private boolean[][] parseOffered(ArpgObjectStruct appearanceArpg, String sourceName) {

        boolean[][] offered = new boolean[AppearanceOptionKind.VALUES.length][];
        ArpgObjectStruct playerArpg = ArpgUtility.hasObject(appearanceArpg, "player")
                ? appearanceArpg.getAsObject("player")
                : new ArpgObjectStruct();

        for (String key : playerArpg.keySet())
            if (findFlaggedKind(key) == null)
                throwException("Appearance \"player\" declares unknown block \"" + key + "\". Source: " + sourceName);

        for (AppearanceOptionKind kind : AppearanceOptionKind.VALUES) {

            if (!kind.isFlagged())
                continue;

            Enum<?>[] members = kind.getMembers();
            ArpgObjectStruct blockArpg = ArpgUtility.hasObject(playerArpg, kind.getFlagBlock())
                    ? playerArpg.getAsObject(kind.getFlagBlock())
                    : new ArpgObjectStruct();

            validateKeys(blockArpg, members, "player." + kind.getFlagBlock(), sourceName);

            if (kind == AppearanceOptionKind.DETAIL)
                validateFreeDetails(blockArpg, sourceName);
            offered[kind.ordinal()] = new boolean[members.length];

            for (int i = 0; i < members.length; i++)
                offered[kind.ordinal()][i] = ArpgUtility.getBoolean(
                        blockArpg, ArpgUtility.toEnumName(members[i]), true);
        }

        return offered;
    }

    // A detail an overlay shows is offered with that overlay, never flagged on its own
    private void validateFreeDetails(ArpgObjectStruct detailsArpg, String sourceName) {

        for (SkinnedDetail skinnedDetail : SkinnedDetail.VALUES)
            if (detailsArpg.has(ArpgUtility.toEnumName(skinnedDetail))
                    && SkinnedOverlay.findByDetail(skinnedDetail) != null)
                throwException("Appearance \"player.details\" flags \"" + ArpgUtility.toEnumName(skinnedDetail)
                        + "\", the amount of an overlay, which is offered with its overlay under \"player.overlays\". "
                        + "Source: " + sourceName);
    }

    private AppearanceOptionKind findFlaggedKind(String flagBlock) {

        for (AppearanceOptionKind kind : AppearanceOptionKind.VALUES)
            if (kind.isFlagged() && kind.getFlagBlock().equals(flagBlock))
                return kind;

        return null;
    }

    // The creator's tabs as the race lays them out, or the Body and Look tabs, the last gaining a section for every
    // offered option and slider group the layout leaves out
    private AppearanceTabStruct[] parseCreator(
            ArpgObjectStruct appearanceArpg,
            AppearanceShapeStruct[] shapes,
            boolean[][] offered,
            String sourceName) {

        ObjectArrayList<AppearanceTabStruct> tabs = new ObjectArrayList<>();

        if (ArpgUtility.hasArray(appearanceArpg, "creator"))
            for (ArpgElementStruct tabElement : appearanceArpg.getAsArray("creator"))
                tabs.add(parseTab(tabElement.getAsObject(), shapes, sourceName));
        else
            addDefaultTabs(tabs);

        if (tabs.isEmpty())
            throwException("Appearance \"creator\" must list at least one tab. Source: " + sourceName);

        ObjectArrayList<AppearanceSectionStruct> leftovers = new ObjectArrayList<>();

        collectLeftoverGroups(tabs, shapes, leftovers);
        collectLeftoverMembers(tabs, offered, leftovers);

        if (leftovers.isEmpty())
            return tabs.toArray(new AppearanceTabStruct[0]);

        AppearanceTabStruct last = tabs.get(tabs.size() - 1);
        AppearanceSectionStruct[] sections = new AppearanceSectionStruct[last.getSectionCount() + leftovers.size()];

        for (int i = 0; i < last.getSectionCount(); i++)
            sections[i] = last.getSection(i);

        for (int i = 0; i < leftovers.size(); i++)
            sections[last.getSectionCount() + i] = leftovers.get(i);

        tabs.set(tabs.size() - 1, new AppearanceTabStruct(last.getTabName(), sections));
        return tabs.toArray(new AppearanceTabStruct[0]);
    }

    private AppearanceTabStruct parseTab(ArpgObjectStruct tabArpg, AppearanceShapeStruct[] shapes, String sourceName) {

        String tabName = ArpgUtility.validateString(tabArpg, "name");
        ArpgArrayStruct sectionsArpg = ArpgUtility.validateArray(tabArpg, "sections");
        AppearanceSectionStruct[] sections = new AppearanceSectionStruct[sectionsArpg.size()];

        for (int i = 0; i < sections.length; i++)
            sections[i] = parseSection(sectionsArpg.get(i).getAsObject(), shapes, tabName, sourceName);

        return new AppearanceTabStruct(tabName, sections);
    }

    private AppearanceSectionStruct parseSection(
            ArpgObjectStruct sectionArpg,
            AppearanceShapeStruct[] shapes,
            String tabName,
            String sourceName) {

        String sectionName = ArpgUtility.validateString(sectionArpg, "name");
        ArpgArrayStruct optionsArpg = ArpgUtility.validateArray(sectionArpg, "options");
        AppearanceOptionStruct[] options = new AppearanceOptionStruct[optionsArpg.size()];

        for (int i = 0; i < options.length; i++)
            options[i] = parseOption(optionsArpg.get(i).getAsObject(), shapes,
                    tabName + " / " + sectionName, sourceName);

        return new AppearanceSectionStruct(sectionName, options);
    }

    // An option of a kind that names a member by its lower-case enum name, a slider group by its name, or nothing
    private AppearanceOptionStruct parseOption(
            ArpgObjectStruct optionArpg,
            AppearanceShapeStruct[] shapes,
            String sectionPath,
            String sourceName) {

        AppearanceOptionKind kind = ArpgUtility.toEnum(
                ArpgUtility.validateString(optionArpg, "kind"), AppearanceOptionKind.class);
        String name = ArpgUtility.getString(optionArpg, "name", "");

        if (kind.isFlagged())
            return new AppearanceOptionStruct(kind, findMember(kind, name, sectionPath, sourceName), null);

        if (kind != AppearanceOptionKind.SLIDERS)
            return new AppearanceOptionStruct(kind, EngineSetting.INDEX_NOT_FOUND, null);

        if (!hasGroup(shapes, name))
            throwException("Creator section \"" + sectionPath + "\" lists slider group \"" + name
                    + "\", which no slider of this character is in. Source: " + sourceName);

        return new AppearanceOptionStruct(kind, EngineSetting.INDEX_NOT_FOUND, name);
    }

    private int findMember(AppearanceOptionKind kind, String name, String sectionPath, String sourceName) {

        Enum<?>[] members = kind.getMembers();

        for (int i = 0; i < members.length; i++)
            if (ArpgUtility.toEnumName(members[i]).equals(name))
                return i;

        throwException("Creator section \"" + sectionPath + "\" lists " + ArpgUtility.toEnumName(kind) + " \""
                + name + "\", which is not one. Source: " + sourceName);
        return EngineSetting.INDEX_NOT_FOUND;
    }

    private boolean hasGroup(AppearanceShapeStruct[] shapes, String groupName) {

        for (int i = 0; i < shapes.length; i++)
            if (shapes[i].getGroupName().equals(groupName))
                return true;

        return false;
    }

    // The layout of a race that gives none: the body and its build first, the rest of the look after
    private void addDefaultTabs(ObjectArrayList<AppearanceTabStruct> tabs) {

        tabs.add(new AppearanceTabStruct(EngineSetting.APPEARANCE_CREATOR_TAB_BODY, new AppearanceSectionStruct[] {
                toSingleSection(EngineSetting.APPEARANCE_CREATOR_SECTION_RACE, AppearanceOptionKind.RACE),
                toSingleSection(EngineSetting.APPEARANCE_CREATOR_SECTION_GENDER, AppearanceOptionKind.GENDER),
                toSingleSection(EngineSetting.APPEARANCE_CREATOR_SECTION_BUILD, AppearanceOptionKind.BUILD),
                toSingleSection(EngineSetting.APPEARANCE_CREATOR_SECTION_HEAD, AppearanceOptionKind.HEAD) }));
        tabs.add(new AppearanceTabStruct(EngineSetting.APPEARANCE_CREATOR_TAB_LOOK, new AppearanceSectionStruct[0]));
    }

    private AppearanceSectionStruct toSingleSection(String sectionName, AppearanceOptionKind kind) {
        return new AppearanceSectionStruct(sectionName, new AppearanceOptionStruct[] {
                new AppearanceOptionStruct(kind, EngineSetting.INDEX_NOT_FOUND, null) });
    }

    // A section for each group holding a slider the player is offered that no section lists, in slider order
    private void collectLeftoverGroups(
            ObjectArrayList<AppearanceTabStruct> tabs,
            AppearanceShapeStruct[] shapes,
            ObjectArrayList<AppearanceSectionStruct> leftovers) {

        ObjectArrayList<String> groups = new ObjectArrayList<>();

        for (int i = 0; i < shapes.length; i++) {

            String groupName = shapes[i].getGroupName();

            if (shapes[i].isShownToPlayer() && !groups.contains(groupName) && !listsGroup(tabs, groupName))
                groups.add(groupName);
        }

        for (int i = 0; i < groups.size(); i++)
            leftovers.add(new AppearanceSectionStruct(groups.get(i), new AppearanceOptionStruct[] {
                    new AppearanceOptionStruct(AppearanceOptionKind.SLIDERS, EngineSetting.INDEX_NOT_FOUND,
                            groups.get(i)) }));
    }

    private boolean listsGroup(ObjectArrayList<AppearanceTabStruct> tabs, String groupName) {

        for (int t = 0; t < tabs.size(); t++)
            for (int s = 0; s < tabs.get(t).getSectionCount(); s++) {

                AppearanceSectionStruct section = tabs.get(t).getSection(s);

                for (int o = 0; o < section.getOptionCount(); o++)
                    if (section.getOption(o).getKind() == AppearanceOptionKind.SLIDERS
                            && section.getOption(o).getGroupName().equals(groupName))
                        return true;
            }

        return false;
    }

    // A section per flagged kind holding every member the player is offered that no section lists, in member order
    private void collectLeftoverMembers(
            ObjectArrayList<AppearanceTabStruct> tabs,
            boolean[][] offered,
            ObjectArrayList<AppearanceSectionStruct> leftovers) {

        ObjectArrayList<AppearanceOptionStruct> options = new ObjectArrayList<>();

        for (AppearanceOptionKind kind : AppearanceOptionKind.VALUES) {

            if (!kind.isFlagged())
                continue;

            options.clear();

            for (int i = 0; i < kind.getMembers().length; i++)
                if (offered[kind.ordinal()][i] && !listsMember(tabs, kind, i) && !isOverlayAmount(kind, i))
                    options.add(new AppearanceOptionStruct(kind, i, null));

            if (!options.isEmpty())
                leftovers.add(new AppearanceSectionStruct(
                        kind.getLeftoverSection(), options.toArray(new AppearanceOptionStruct[0])));
        }
    }

    // A detail that is an overlay's amount, offered with its overlay rather than on its own
    private boolean isOverlayAmount(AppearanceOptionKind kind, int index) {
        return kind == AppearanceOptionKind.DETAIL && SkinnedOverlay.findByDetail(SkinnedDetail.VALUES[index]) != null;
    }

    private boolean listsMember(ObjectArrayList<AppearanceTabStruct> tabs, AppearanceOptionKind kind, int index) {

        for (int t = 0; t < tabs.size(); t++)
            for (int s = 0; s < tabs.get(t).getSectionCount(); s++) {

                AppearanceSectionStruct section = tabs.get(t).getSection(s);

                for (int o = 0; o < section.getOptionCount(); o++)
                    if (section.getOption(o).getKind() == kind && section.getOption(o).getIndex() == index)
                        return true;
            }

        return false;
    }

    private <E extends Enum<E>> void validateKeys(
            ArpgObjectStruct blockArpg,
            Class<E> enumClass,
            String blockName,
            String sourceName) {

        validateKeys(blockArpg, enumClass.getEnumConstants(), blockName, sourceName);
    }

    private void validateKeys(
            ArpgObjectStruct blockArpg,
            Enum<?>[] constants,
            String blockName,
            String sourceName) {

        for (String key : blockArpg.keySet()) {

            boolean known = false;

            for (Enum<?> constant : constants)
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
