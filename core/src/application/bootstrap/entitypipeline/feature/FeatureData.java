package application.bootstrap.entitypipeline.feature;

import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import application.bootstrap.shaderpipeline.texture.TextureHandle;
import engine.root.DataPackage;

public class FeatureData extends DataPackage {

    /*
     * Immutable definition of one selectable appearance option. meshHandle
     * is set only for mesh slots, textureHandle only for texture slots, and
     * faceTextureHandle only for the head — the tile its front face maps to
     * and every face overlay is painted across. Owned by FeatureHandle in
     * FeatureManager's palette for the engine lifetime.
     */

    // Identity
    private final String featureName;
    private final short featureID;
    private final FeatureSlot featureSlot;

    // Content
    private final MeshHandle meshHandle;
    private final TextureHandle textureHandle;
    private final TextureHandle faceTextureHandle;

    // Constructor \\

    public FeatureData(
            String featureName,
            short featureID,
            FeatureSlot featureSlot,
            MeshHandle meshHandle,
            TextureHandle textureHandle,
            TextureHandle faceTextureHandle) {

        // Identity
        this.featureName = featureName;
        this.featureID = featureID;
        this.featureSlot = featureSlot;

        // Content
        this.meshHandle = meshHandle;
        this.textureHandle = textureHandle;
        this.faceTextureHandle = faceTextureHandle;
    }

    // Accessible \\

    public String getFeatureName() {
        return featureName;
    }

    public short getFeatureID() {
        return featureID;
    }

    public FeatureSlot getFeatureSlot() {
        return featureSlot;
    }

    public MeshHandle getMeshHandle() {
        return meshHandle;
    }

    public TextureHandle getTextureHandle() {
        return textureHandle;
    }

    public TextureHandle getFaceTextureHandle() {
        return faceTextureHandle;
    }
}
