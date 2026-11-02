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

public class AppearanceData extends DataPackage {

    /*
     * Immutable appearance template for one entity type: the character mesh
     * whose morph targets its sliders weigh, the default color and palette of
     * every SkinnedColor, the default amount of every SkinnedDetail, the
     * shape sliders, the morphs each AppearanceExpression plays, the default
     * feature per slot, the head-shape bone and the build curve.
     * isCompatible() is the one rule for whether a feature can be worn.
     */

    // Rig
    private final RigHandle rigHandle;
    private final int headBoneIndex;

    // Mesh
    private final MeshHandle characterMesh;

    // Colors — indexed by SkinnedColor ordinal
    private final Color[] colors;
    private final Color[][] palettes;

    // Details — indexed by SkinnedDetail ordinal
    private final float[] details;

    // Shapes
    private final AppearanceShapeStruct[] shapes;

    // Expressions — morph index per AppearanceExpression ordinal, INDEX_NOT_FOUND when the mesh has none
    private final int[] expressionMorphs;

    // Features — indexed by FeatureSlot ordinal
    private final FeatureHandle[] defaultFeatures;

    // Build
    private final float thinBuildFactor;
    private final float heavyBuildFactor;
    private final float[] girthInfluence;

    // Constructor \\

    public AppearanceData(
            RigHandle rigHandle,
            int headBoneIndex,
            MeshHandle characterMesh,
            Color[] colors,
            Color[][] palettes,
            float[] details,
            AppearanceShapeStruct[] shapes,
            int[] expressionMorphs,
            FeatureHandle[] defaultFeatures,
            float thinBuildFactor,
            float heavyBuildFactor,
            float[] girthInfluence) {

        // Rig
        this.rigHandle = rigHandle;
        this.headBoneIndex = headBoneIndex;

        // Mesh
        this.characterMesh = characterMesh;

        // Colors
        this.colors = colors;
        this.palettes = palettes;

        // Details
        this.details = details;

        // Shapes
        this.shapes = shapes;

        // Expressions
        this.expressionMorphs = expressionMorphs;

        // Features
        this.defaultFeatures = defaultFeatures;

        // Build
        this.thinBuildFactor = thinBuildFactor;
        this.heavyBuildFactor = heavyBuildFactor;
        this.girthInfluence = girthInfluence;
    }

    // Compatibility \\

    public boolean isCompatible(FeatureHandle featureHandle) {
        return featureHandle.getMeshHandle().getRigHandle() == rigHandle;
    }

    // Build \\

    public float getBuildFactor(float weightRatio) {
        return thinBuildFactor + (heavyBuildFactor - thinBuildFactor) * weightRatio;
    }

    // Shapes \\

    public int getShapeCount() {
        return shapes.length;
    }

    public AppearanceShapeStruct getShape(int shapeIndex) {
        return shapes[shapeIndex];
    }

    // The index of the slider weighing a morph, INDEX_NOT_FOUND when no slider does
    public int findShape(String morphName) {

        for (int i = 0; i < shapes.length; i++)
            if (shapes[i].getMorphName().equals(morphName))
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    // Accessible \\

    public RigHandle getRigHandle() {
        return rigHandle;
    }

    public int getHeadBoneIndex() {
        return headBoneIndex;
    }

    public MeshHandle getCharacterMesh() {
        return characterMesh;
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

    public int getExpressionMorph(AppearanceExpression expression) {
        return expressionMorphs[expression.ordinal()];
    }

    public FeatureHandle getDefaultFeature(FeatureSlot featureSlot) {
        return defaultFeatures[featureSlot.ordinal()];
    }

    public float getGirthInfluence(int boneIndex) {
        return girthInfluence[boneIndex];
    }
}
