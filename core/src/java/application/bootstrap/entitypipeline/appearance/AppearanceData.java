package application.bootstrap.entitypipeline.appearance;

import application.bootstrap.entitypipeline.feature.FeatureHandle;
import application.bootstrap.entitypipeline.feature.FeatureSlot;
import application.bootstrap.entitypipeline.util.BoneControlCurveUtility;
import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedBoneMaskStruct;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedColor;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedDetail;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedMaskMode;
import engine.graphics.color.Color;
import engine.root.DataPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class AppearanceData extends DataPackage {

    /*
     * Immutable appearance of one race: the body meshes its genders wear —
     * in the order the race lists them, every one on the race's rig and
     * carrying the same morphs — with the morph each slider weighs resolved
     * on every body, the expressions its face plays (each a clip of face
     * bones and morph curves, the one named blink played by itself), the default
     * color and palette of every SkinnedColor, the default amount of every
     * SkinnedDetail, the shape sliders, the default feature per slot, each
     * gender's starting preset, the head-shape bone with the mask that hides
     * it and every bone below it from a character's own eyes, and the build
     * curve, with the character creator's layout (its tabs, their sections
     * and the options each lists) and the player flags of every color,
     * detail, overlay and feature.
     * isCompatible() is the one rule for whether a feature can be worn, and
     * isOffered() for whether the player is given an option.
     */

    // Rig
    private final RigHandle rigHandle;
    private final int headBoneIndex;
    private final SkinnedBoneMaskStruct headMask;

    // Bodies — in the order the race's genders wear them
    private final MeshHandle[] bodyMeshes;
    private final int maxMorphCount;

    // Colors — indexed by SkinnedColor ordinal
    private final Color[] colors;
    private final Color[][] palettes;

    // Details — indexed by SkinnedDetail ordinal
    private final float[] details;

    // Shapes — the sliders, and the morph each weighs per body, INDEX_NOT_FOUND for one that drives only bones
    private final AppearanceShapeStruct[] shapes;
    private final int[][] shapeMorphs;

    // Expressions — in the order the race names them, and the blink's place among them, INDEX_NOT_FOUND for none
    private final AppearanceExpressionStruct[] expressions;
    private final int blinkExpression;

    // Features — indexed by FeatureSlot ordinal
    private final FeatureHandle[] defaultFeatures;

    // Genders — the look each gender starts with, by gender ID
    private final Int2ObjectOpenHashMap<AppearanceGenderStruct> genderID2Preset;

    // Build
    private final float thinBuildFactor;
    private final float heavyBuildFactor;
    private final float[] girthInfluence;

    // Creator — the tabs in order, and whether the player is offered each member, by AppearanceOptionKind ordinal
    // then member ordinal, null for a kind that names no member
    private final AppearanceTabStruct[] creatorTabs;
    private final boolean[][] offered;

    // Constructor \\

    public AppearanceData(
            RigHandle rigHandle,
            int headBoneIndex,
            MeshHandle[] bodyMeshes,
            Color[] colors,
            Color[][] palettes,
            float[] details,
            AppearanceShapeStruct[] shapes,
            int[][] shapeMorphs,
            AppearanceExpressionStruct[] expressions,
            FeatureHandle[] defaultFeatures,
            Int2ObjectOpenHashMap<AppearanceGenderStruct> genderID2Preset,
            float thinBuildFactor,
            float heavyBuildFactor,
            float[] girthInfluence,
            AppearanceTabStruct[] creatorTabs,
            boolean[][] offered) {

        // Rig
        this.rigHandle = rigHandle;
        this.headBoneIndex = headBoneIndex;
        this.headMask = new SkinnedBoneMaskStruct();
        headMask.setMode(SkinnedMaskMode.HIDE);
        headMask.addBranch(rigHandle, headBoneIndex);

        // Bodies
        this.bodyMeshes = bodyMeshes;
        this.maxMorphCount = resolveMaxMorphCount(bodyMeshes);

        // Colors
        this.colors = colors;
        this.palettes = palettes;

        // Details
        this.details = details;

        // Shapes
        this.shapes = shapes;
        this.shapeMorphs = shapeMorphs;

        // Expressions
        this.expressions = expressions;
        this.blinkExpression = findExpression(EngineSetting.FACE_EXPRESSION_BLINK);

        // Features
        this.defaultFeatures = defaultFeatures;

        // Genders
        this.genderID2Preset = genderID2Preset;

        // Build
        this.thinBuildFactor = thinBuildFactor;
        this.heavyBuildFactor = heavyBuildFactor;
        this.girthInfluence = girthInfluence;

        // Creator
        this.creatorTabs = creatorTabs;
        this.offered = offered;
    }

    private static int resolveMaxMorphCount(MeshHandle[] bodyMeshes) {

        int count = 0;

        for (int i = 0; i < bodyMeshes.length; i++)
            count = Math.max(count, bodyMeshes[i].getMorphCount());

        return count;
    }

    // Compatibility \\

    public boolean isCompatible(FeatureHandle featureHandle) {
        return featureHandle.getMeshHandle().getRigHandle() == rigHandle;
    }

    // Build \\

    public float getBuildFactor(float weightRatio) {
        return thinBuildFactor + (heavyBuildFactor - thinBuildFactor) * weightRatio;
    }

    // Bodies \\

    public int getBodyCount() {
        return bodyMeshes.length;
    }

    public MeshHandle getBodyMesh(int bodyIndex) {
        return bodyMeshes[bodyIndex];
    }

    // The body a gender wears, the first body for a gender the race does not allow
    public int getBodyIndex(short genderID) {

        AppearanceGenderStruct preset = genderID2Preset.get(genderID);
        return preset != null ? preset.getBodyIndex() : 0;
    }

    // The most morphs any body carries, so one weight array serves every body
    public int getMaxMorphCount() {
        return maxMorphCount;
    }

    // Shapes \\

    public int getShapeCount() {
        return shapes.length;
    }

    public AppearanceShapeStruct getShape(int shapeIndex) {
        return shapes[shapeIndex];
    }

    // The morph a slider weighs on a body, INDEX_NOT_FOUND for a slider that drives only bones
    public int getShapeMorph(int bodyIndex, int shapeIndex) {
        return shapeMorphs[bodyIndex][shapeIndex];
    }

    // The value a slider starts at for a gender: its preset's, else the slider's default, held to the gender's range
    public float getStartingShapeValue(short genderID, int shapeIndex) {

        AppearanceGenderStruct preset = genderID2Preset.get(genderID);
        float value = preset != null ? preset.getShapeValue(shapeIndex) : Float.NaN;

        return Math.clamp(
                Float.isNaN(value) ? shapes[shapeIndex].getDefaultValue() : value,
                getShapeMin(genderID, shapeIndex),
                getShapeMax(genderID, shapeIndex));
    }

    // The low end a gender may slide a slider to: its preset's range where it gives one, else the slider's own
    public float getShapeMin(short genderID, int shapeIndex) {

        AppearanceGenderStruct preset = genderID2Preset.get(genderID);
        float min = preset != null ? preset.getShapeMin(shapeIndex) : Float.NaN;

        return Float.isNaN(min) ? shapes[shapeIndex].getMin() : min;
    }

    // The high end a gender may slide a slider to: its preset's range where it gives one, else the slider's own
    public float getShapeMax(short genderID, int shapeIndex) {

        AppearanceGenderStruct preset = genderID2Preset.get(genderID);
        float max = preset != null ? preset.getShapeMax(shapeIndex) : Float.NaN;

        return Float.isNaN(max) ? shapes[shapeIndex].getMax() : max;
    }

    // A control's push on one bone slider it drives, at a value of the control: how far the drive's curve has carried,
    // across the control's own range, from zero to the value, as a share of the slider's own range, down that range
    // for a slider that falls with the control; scratch holds BONE_CONTROL_CURVE_SCRATCH_FLOATS floats
    public float resolvePush(int controlIndex, int driveIndex, float controlValue, float[] scratch) {

        AppearanceShapeStruct control = shapes[controlIndex];
        AppearanceDriveStruct drive = control.getDrive(driveIndex);
        AppearanceShapeStruct slider = shapes[drive.getSliderIndex()];
        float span = control.getMax() - control.getMin();

        if (span <= 0f)
            return 0f;

        float share = BoneControlCurveUtility.evaluate(drive.getKeys(), toPlace(controlIndex, controlValue), scratch)
                - BoneControlCurveUtility.evaluate(drive.getKeys(), getZeroPlace(controlIndex), scratch);

        return (drive.isIncrease() ? share : -share) * (slider.getMax() - slider.getMin());
    }

    // Where a value of a slider stands along its own range, 0 at its low end to 1 at its high end
    public float toPlace(int shapeIndex, float value) {

        AppearanceShapeStruct shape = shapes[shapeIndex];
        float span = shape.getMax() - shape.getMin();

        return span > 0f ? Math.clamp((value - shape.getMin()) / span, 0f, 1f) : 0f;
    }

    // Where zero stands along a control's range, the place it pushes nothing from
    public float getZeroPlace(int shapeIndex) {
        return toPlace(shapeIndex, 0f);
    }

    // The index of the slider of a name, INDEX_NOT_FOUND when no slider has it
    public int findShape(String shapeName) {

        for (int i = 0; i < shapes.length; i++)
            if (shapes[i].getShapeName().equals(shapeName))
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    // Features \\

    // The feature a gender starts with in a slot: the one its preset names, null where it names none, else the race's
    // default
    public FeatureHandle getStartingFeature(short genderID, FeatureSlot featureSlot) {

        AppearanceGenderStruct preset = genderID2Preset.get(genderID);
        int slotIndex = featureSlot.ordinal();

        return preset != null && preset.namesFeature(slotIndex)
                ? preset.getFeature(slotIndex)
                : defaultFeatures[slotIndex];
    }

    // Accessible \\

    public RigHandle getRigHandle() {
        return rigHandle;
    }

    public int getHeadBoneIndex() {
        return headBoneIndex;
    }

    // Hides the head and everything it carries, the eyes and teeth worn in it included
    public SkinnedBoneMaskStruct getHeadMask() {
        return headMask;
    }

    public Color getColor(SkinnedColor skinnedColor) {
        return colors[skinnedColor.ordinal()];
    }

    public Color[] getPalette(SkinnedColor skinnedColor) {
        return palettes[skinnedColor.ordinal()];
    }

    public float getDetail(SkinnedDetail skinnedDetail) {
        return details[skinnedDetail.ordinal()];
    }

    public int getExpressionCount() {
        return expressions.length;
    }

    public AppearanceExpressionStruct getExpression(int expressionIndex) {
        return expressions[expressionIndex];
    }

    // An expression's place by its name, INDEX_NOT_FOUND when the race names none so
    public int findExpression(String expressionName) {

        for (int i = 0; i < expressions.length; i++)
            if (expressions[i].getExpressionName().equals(expressionName))
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    // The expression that blinks by itself, INDEX_NOT_FOUND when the race names none
    public int getBlinkExpression() {
        return blinkExpression;
    }

    public int getCreatorTabCount() {
        return creatorTabs.length;
    }

    public AppearanceTabStruct getCreatorTab(int index) {
        return creatorTabs[index];
    }

    // Whether the player is offered a member of a flagged kind; every option of an unflagged kind is
    public boolean isOffered(AppearanceOptionKind kind, int index) {
        return !kind.isFlagged() || offered[kind.ordinal()][index];
    }

    public FeatureHandle getDefaultFeature(FeatureSlot featureSlot) {
        return defaultFeatures[featureSlot.ordinal()];
    }

    // The preset of a gender, null when the race gives it none
    public AppearanceGenderStruct getGenderPreset(short genderID) {
        return genderID2Preset.get(genderID);
    }

    // How many of the rig's bones the appearance was built against, those it shapes; a bone added after stands as
    // the rig places it
    public int getBoneCount() {
        return girthInfluence.length;
    }

    public float getGirthInfluence(int boneIndex) {
        return girthInfluence[boneIndex];
    }
}
