package application.bootstrap.entitypipeline.appearance;

import application.bootstrap.entitypipeline.animationtree.AnimationStateHandle;
import application.bootstrap.entitypipeline.feature.FeatureHandle;
import application.bootstrap.entitypipeline.feature.FeatureSlot;
import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.shaderpipeline.texture.TextureHandle;
import engine.graphics.color.Color;
import engine.root.HandlePackage;
import engine.util.mathematics.vectors.Vector3;

public class AppearanceHandle extends HandlePackage {

    /*
     * Per-entity appearance: skin and hair color, worn features, body build and
     * head shape. Seeded from the template and edited in place;
     * applyProportions() is the one path that writes bone proportions and
     * re-poses the entity, and resetToDefaults() returns everything to the
     * template. Lives on EntityInstance.
     */

    // Internal
    private AppearanceData appearanceData;
    private AnimationStateHandle animationStateHandle;

    // Colors
    private Color skinColor;
    private Color hairColor;

    // Features — indexed by FeatureSlot ordinal
    private FeatureHandle[] features;

    // Proportions
    private float weightRatio;
    private Vector3 headProportion;

    // Constructor \\

    public void constructor(
            AppearanceData appearanceData,
            AnimationStateHandle animationStateHandle,
            float weightRatio) {

        // Internal
        this.appearanceData = appearanceData;
        this.animationStateHandle = animationStateHandle;

        // Colors
        this.skinColor = new Color();
        this.hairColor = new Color();

        // Features
        this.features = new FeatureHandle[FeatureSlot.VALUES.length];

        // Proportions
        this.weightRatio = weightRatio;
        this.headProportion = new Vector3();

        resetToDefaults();
    }

    // Defaults \\

    public void resetToDefaults() {

        skinColor.set(appearanceData.getSkinColor());
        hairColor.set(appearanceData.getHairColor());

        for (FeatureSlot featureSlot : FeatureSlot.VALUES)
            features[featureSlot.ordinal()] = appearanceData.getDefaultFeature(featureSlot);

        headProportion.set(1f, 1f, 1f);

        applyProportions();
    }

    // Colors \\

    public Color getSkinColor() {
        return skinColor;
    }

    public void setSkinColor(float r, float g, float b) {
        skinColor.set(r, g, b);
    }

    public Color getHairColor() {
        return hairColor;
    }

    public void setHairColor(float r, float g, float b) {
        hairColor.set(r, g, b);
    }

    // Features \\

    public FeatureHandle getFeature(FeatureSlot featureSlot) {
        return features[featureSlot.ordinal()];
    }

    public boolean hasFeature(FeatureSlot featureSlot) {
        return features[featureSlot.ordinal()] != null;
    }

    public void setFeature(FeatureHandle featureHandle) {

        if (!appearanceData.isCompatible(featureHandle))
            throwException("Feature \"" + featureHandle.getFeatureName()
                    + "\" does not fit this character — its mesh must use the character's rig and its "
                    + "textures must share the character's texture array.");

        features[featureHandle.getFeatureSlot().ordinal()] = featureHandle;
    }

    public void clearFeature(FeatureSlot featureSlot) {

        if (featureSlot.isRequired())
            throwException("Feature slot " + featureSlot + " is required and cannot be left empty.");

        features[featureSlot.ordinal()] = null;
    }

    public MeshHandle getHeadMesh() {
        return getFeature(FeatureSlot.HEAD).getMeshHandle();
    }

    public TextureHandle getFaceTexture() {
        return getFeature(FeatureSlot.HEAD).getFaceTextureHandle();
    }

    public TextureHandle getFeatureTexture(FeatureSlot featureSlot) {

        FeatureHandle featureHandle = getFeature(featureSlot);

        return featureHandle != null ? featureHandle.getTextureHandle() : null;
    }

    // Proportions \\

    public float getWeightRatio() {
        return weightRatio;
    }

    public void setWeightRatio(float weightRatio) {
        this.weightRatio = weightRatio;
        applyProportions();
    }

    public Vector3 getHeadProportion() {
        return headProportion;
    }

    public void setHeadProportion(float width, float height, float depth) {
        headProportion.set(width, height, depth);
        applyProportions();
    }

    private void applyProportions() {

        float buildFactor = appearanceData.getBuildFactor(weightRatio);
        int headBoneIndex = appearanceData.getHeadBoneIndex();
        int boneCount = appearanceData.getRigHandle().getBoneCount();

        for (int i = 0; i < boneCount; i++) {

            float girth = 1f + (buildFactor - 1f) * appearanceData.getGirthInfluence(i);

            if (i == headBoneIndex)
                animationStateHandle.setBoneProportion(
                        i,
                        girth * headProportion.x,
                        headProportion.y,
                        girth * headProportion.z);
            else
                animationStateHandle.setBoneProportion(i, girth, 1f, girth);
        }

        animationStateHandle.refreshPose();
    }
}
