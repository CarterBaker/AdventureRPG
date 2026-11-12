package application.bootstrap.geometrypipeline.skinnedbuffer;

import java.util.Arrays;

import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import engine.graphics.color.Color;
import engine.root.EngineSetting;
import engine.root.StructPackage;

public class SkinnedAppearanceStruct extends StructPackage {

    /*
     * The appearance one skinned draw writes into its instance's palette row
     * after the bones: a header texel (x = SkinnedMaskMode), one texel per
     * SkinnedColor, the SkinnedDetail amounts four to a texel, the bone mask
     * and three texels per SkinnedOverlay placing it on the model drawn,
     * followed by the morph weights of the mesh they were resolved for — any
     * other mesh drawn with this appearance, hair worn on the body, gets zero
     * weights.
     * Reused as a mutable scratch by whoever submits skinned draws, holding
     * the weights array it is lent, never a copy.
     */

    // Texels
    private final float[] texels;

    // Morphs
    private MeshHandle morphMesh;
    private float[] morphWeights;

    // Constructor \\

    public SkinnedAppearanceStruct() {

        // Texels
        this.texels = new float[getTexelCount() * EngineSetting.COLOR_CHANNEL_COUNT];

        reset();
    }

    // Management \\

    public void reset() {

        Arrays.fill(texels, 0f);

        for (SkinnedColor skinnedColor : SkinnedColor.VALUES)
            setColor(skinnedColor, Color.WHITE);

        morphMesh = null;
        morphWeights = null;
    }

    // The bones this draw hides or alone shows, copied in whole
    public void setBoneMask(SkinnedBoneMaskStruct boneMask) {
        texels[0] = boneMask.getMode().ordinal();
        boneMask.write(texels, getMaskTexel() * EngineSetting.COLOR_CHANNEL_COUNT);
    }

    // Where an overlay lies on the model drawn; null where the model places none, so it paints nothing
    public void setOverlay(SkinnedOverlay skinnedOverlay, SkinnedOverlayStruct overlay) {

        int base = (getMaskTexel() + EngineSetting.SKINNED_BONE_MASK_TEXELS
                + skinnedOverlay.ordinal() * EngineSetting.SKINNED_OVERLAY_TEXELS) * EngineSetting.COLOR_CHANNEL_COUNT;

        if (overlay != null) {
            overlay.write(texels, base);
            return;
        }

        Arrays.fill(
                texels,
                base,
                base + EngineSetting.SKINNED_OVERLAY_TEXELS * EngineSetting.COLOR_CHANNEL_COUNT,
                0f);
    }

    public void setColor(SkinnedColor skinnedColor, Color color) {

        int base = (EngineSetting.SKINNED_APPEARANCE_HEADER_TEXELS + skinnedColor.ordinal())
                * EngineSetting.COLOR_CHANNEL_COUNT;

        texels[base] = color.r;
        texels[base + 1] = color.g;
        texels[base + 2] = color.b;
        texels[base + 3] = color.a;
    }

    public void setDetail(SkinnedDetail skinnedDetail, float amount) {
        texels[(EngineSetting.SKINNED_APPEARANCE_HEADER_TEXELS + SkinnedColor.VALUES.length)
                * EngineSetting.COLOR_CHANNEL_COUNT + skinnedDetail.ordinal()] = amount;
    }

    // Lends the morph weights resolved for one mesh, by that mesh's morph index
    public void setMorphWeights(MeshHandle meshHandle, float[] weights) {
        this.morphMesh = meshHandle;
        this.morphWeights = weights;
    }

    // Write \\

    public void write(float[] out, int offset) {
        System.arraycopy(texels, 0, out, offset, texels.length);
    }

    // The weights of a mesh's morphs, in order; zero for a mesh the weights were not resolved for
    public void writeMorphWeights(MeshHandle meshHandle, float[] out, int offset) {

        int morphCount = meshHandle.getMorphCount();
        boolean resolved = meshHandle == morphMesh && morphWeights != null;

        for (int m = 0; m < morphCount; m++)
            out[offset + m] = resolved && m < morphWeights.length ? morphWeights[m] : 0f;
    }

    // Accessible \\

    public static int getTexelCount() {
        return getMaskTexel() + EngineSetting.SKINNED_BONE_MASK_TEXELS
                + SkinnedOverlay.VALUES.length * EngineSetting.SKINNED_OVERLAY_TEXELS;
    }

    private static int getMaskTexel() {
        return EngineSetting.SKINNED_APPEARANCE_HEADER_TEXELS + SkinnedColor.VALUES.length
                + EngineSetting.SKINNED_APPEARANCE_DETAIL_TEXELS;
    }
}
