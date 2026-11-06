package application.bootstrap.entitypipeline.appearance;

import application.bootstrap.entitypipeline.feature.FeatureHandle;
import application.bootstrap.entitypipeline.feature.FeatureSlot;
import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedColor;
import application.bootstrap.geometrypipeline.skinnedbuffer.SkinnedDetail;
import engine.graphics.color.Color;
import engine.root.DataPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class AppearanceData extends DataPackage {

    /*
     * Immutable appearance template for one entity type: the body meshes its
     * genders wear — the template's own mesh first, every one on the same rig
     * and carrying the same morphs — with the morph each slider weighs and
     * each AppearanceExpression plays resolved on every body, the default
     * color and palette of every SkinnedColor, the default amount of every
     * SkinnedDetail, the shape sliders, the default feature per slot, each
     * gender's starting preset, the head-shape bone and the build curve.
     * isCompatible() is the one rule for whether a feature can be worn.
     */

    // Rig
    private final RigHandle rigHandle;
    private final int headBoneIndex;

    // Bodies — the template's mesh first
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

    // Expressions — morph index per body and AppearanceExpression ordinal, INDEX_NOT_FOUND when the body has none
    private final int[][] expressionMorphs;

    // Features — indexed by FeatureSlot ordinal
    private final FeatureHandle[] defaultFeatures;

    // Genders — the look each gender starts with, by gender ID
    private final Int2ObjectOpenHashMap<AppearanceGenderStruct> genderID2Preset;

    // Build
    private final float thinBuildFactor;
    private final float heavyBuildFactor;
    private final float[] girthInfluence;

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
            int[][] expressionMorphs,
            FeatureHandle[] defaultFeatures,
            Int2ObjectOpenHashMap<AppearanceGenderStruct> genderID2Preset,
            float thinBuildFactor,
            float heavyBuildFactor,
            float[] girthInfluence) {

        // Rig
        this.rigHandle = rigHandle;
        this.headBoneIndex = headBoneIndex;

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
        this.expressionMorphs = expressionMorphs;

        // Features
        this.defaultFeatures = defaultFeatures;

        // Genders
        this.genderID2Preset = genderID2Preset;

        // Build
        this.thinBuildFactor = thinBuildFactor;
        this.heavyBuildFactor = heavyBuildFactor;
        this.girthInfluence = girthInfluence;
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

    // The body a gender wears, the template's own mesh for a gender with no preset or no mesh of its own
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

    // The value a slider starts at for a gender: its preset's, else the slider's default
    public float getStartingShapeValue(short genderID, int shapeIndex) {

        AppearanceGenderStruct preset = genderID2Preset.get(genderID);
        float value = preset != null ? preset.getShapeValue(shapeIndex) : Float.NaN;

        return Float.isNaN(value) ? shapes[shapeIndex].getDefaultValue() : shapes[shapeIndex].clamp(value);
    }

    // The index of the slider of a name, INDEX_NOT_FOUND when no slider has it
    public int findShape(String shapeName) {

        for (int i = 0; i < shapes.length; i++)
            if (shapes[i].getShapeName().equals(shapeName))
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    // Features \\

    // The feature a gender starts with in a slot: its preset's, else the template's default
    public FeatureHandle getStartingFeature(short genderID, FeatureSlot featureSlot) {

        AppearanceGenderStruct preset = genderID2Preset.get(genderID);
        FeatureHandle featureHandle = preset != null ? preset.getFeature(featureSlot.ordinal()) : null;

        return featureHandle != null ? featureHandle : defaultFeatures[featureSlot.ordinal()];
    }

    // Accessible \\

    public RigHandle getRigHandle() {
        return rigHandle;
    }

    public int getHeadBoneIndex() {
        return headBoneIndex;
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

    public int getExpressionMorph(int bodyIndex, AppearanceExpression expression) {
        return expressionMorphs[bodyIndex][expression.ordinal()];
    }

    public FeatureHandle getDefaultFeature(FeatureSlot featureSlot) {
        return defaultFeatures[featureSlot.ordinal()];
    }

    // The preset of a gender, null when the template gives it none
    public AppearanceGenderStruct getGenderPreset(short genderID) {
        return genderID2Preset.get(genderID);
    }

    public float getGirthInfluence(int boneIndex) {
        return girthInfluence[boneIndex];
    }
}
