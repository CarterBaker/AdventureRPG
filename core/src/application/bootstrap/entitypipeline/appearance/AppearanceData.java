package application.bootstrap.entitypipeline.appearance;

import application.bootstrap.entitypipeline.feature.FeatureHandle;
import application.bootstrap.entitypipeline.feature.FeatureSlot;
import application.bootstrap.geometrypipeline.rig.RigHandle;
import engine.graphics.color.Color;
import engine.root.DataPackage;

public class AppearanceData extends DataPackage {

    /*
     * Immutable appearance template for one entity type, loaded from the
     * "appearance" block of its entity JSON: default skin and hair colors,
     * the default feature per slot (null for an empty optional slot), the
     * head bone head-shape proportions apply to, and the build curve that
     * widens each bone between thin and heavy. isCompatible() is the one
     * rule for whether a feature can be worn — its meshes must use this rig
     * and its textures must sit in the default head's face-tile array, the
     * single array the character material samples every overlay from.
     */

    // Rig
    private final RigHandle rigHandle;
    private final int headBoneIndex;
    private final int textureArrayID;

    // Colors
    private final Color skinColor;
    private final Color hairColor;

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
            Color skinColor,
            Color hairColor,
            FeatureHandle[] defaultFeatures,
            float thinBuildFactor,
            float heavyBuildFactor,
            float[] girthInfluence) {

        // Rig
        this.rigHandle = rigHandle;
        this.headBoneIndex = headBoneIndex;
        this.textureArrayID = defaultFeatures[FeatureSlot.HEAD.ordinal()].getFaceTextureHandle().getArrayID();

        // Colors
        this.skinColor = skinColor;
        this.hairColor = hairColor;

        // Features
        this.defaultFeatures = defaultFeatures;

        // Build
        this.thinBuildFactor = thinBuildFactor;
        this.heavyBuildFactor = heavyBuildFactor;
        this.girthInfluence = girthInfluence;
    }

    // Compatibility \\

    public boolean isCompatible(FeatureHandle featureHandle) {

        FeatureSlot featureSlot = featureHandle.getFeatureSlot();

        if (featureSlot.isMeshSlot() && featureHandle.getMeshHandle().getRigHandle() != rigHandle)
            return false;

        if (featureSlot.isFaceSlot() && featureHandle.getFaceTextureHandle().getArrayID() != textureArrayID)
            return false;

        return featureSlot.isMeshSlot() || featureHandle.getTextureHandle().getArrayID() == textureArrayID;
    }

    // Build \\

    public float getBuildFactor(float weightRatio) {
        return thinBuildFactor + (heavyBuildFactor - thinBuildFactor) * weightRatio;
    }

    // Accessible \\

    public RigHandle getRigHandle() {
        return rigHandle;
    }

    public int getHeadBoneIndex() {
        return headBoneIndex;
    }

    public int getTextureArrayID() {
        return textureArrayID;
    }

    public Color getSkinColor() {
        return skinColor;
    }

    public Color getHairColor() {
        return hairColor;
    }

    public FeatureHandle getDefaultFeature(FeatureSlot featureSlot) {
        return defaultFeatures[featureSlot.ordinal()];
    }

    public float getGirthInfluence(int boneIndex) {
        return girthInfluence[boneIndex];
    }
}
