package application.bootstrap.entitypipeline.appearance;

import application.bootstrap.entitypipeline.animation.AnimationStateHandle;
import application.bootstrap.entitypipeline.feature.FeatureHandle;
import application.bootstrap.entitypipeline.feature.FeatureSlot;
import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.shaderpipeline.texture.TextureHandle;
import engine.graphics.color.Color;
import engine.root.HandlePackage;
import engine.util.mathematics.vectors.Vector3;

public class AppearanceHandle extends HandlePackage {

    /*
     * Per-entity runtime appearance — skin and hair color, the feature worn
     * in every slot, the body build derived from the entity's weight, and
     * the head-shape proportion. Seeded from the template's AppearanceData
     * and edited in place by whatever customizes a character; rendering
     * reads it every frame through EntityRenderSystem. Every proportion
     * change routes through applyProportions(), the one place bone
     * proportions are written to the entity's AnimationStateHandle and its
     * pose re-evaluated, so edits show immediately even while the entity's
     * animation is paused. No manager owns this — it lives directly on
     * EntityInstance, same as AnimationStateHandle.
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
        this.skinColor = new Color(appearanceData.getSkinColor());
        this.hairColor = new Color(appearanceData.getHairColor());

        // Features
        this.features = new FeatureHandle[FeatureSlot.values().length];

        for (FeatureSlot featureSlot : FeatureSlot.values())
            features[featureSlot.ordinal()] = appearanceData.getDefaultFeature(featureSlot);

        // Proportions
        this.weightRatio = weightRatio;
        this.headProportion = new Vector3(1f, 1f, 1f);

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
