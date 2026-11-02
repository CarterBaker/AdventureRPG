package application.bootstrap.geometrypipeline.skinnedbuffer;

import application.bootstrap.geometrypipeline.mesh.MeshHandle;
import engine.graphics.color.Color;
import engine.root.EngineSetting;
import engine.root.StructPackage;

public class SkinnedAppearanceStruct extends StructPackage {

    /*
     * The appearance one skinned draw writes into its instance's palette row
     * after the bones: a header texel (x = hidden bone index), one texel per
     * SkinnedColor and the SkinnedDetail amounts four to a texel, followed by
     * the morph weights of the mesh they were resolved for — any other mesh
     * drawn with this appearance, hair worn on the body, gets zero weights.
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

        texels[0] = EngineSetting.SKINNED_HIDDEN_BONE_NONE;
        texels[1] = 0f;
        texels[2] = 0f;
        texels[3] = 0f;

        for (SkinnedColor skinnedColor : SkinnedColor.VALUES)
            setColor(skinnedColor, Color.WHITE);

        for (SkinnedDetail skinnedDetail : SkinnedDetail.VALUES)
            setDetail(skinnedDetail, 0f);

        morphMesh = null;
        morphWeights = null;
    }

    public void setHiddenBone(float hiddenBone) {
        texels[0] = hiddenBone;
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
        return EngineSetting.SKINNED_APPEARANCE_HEADER_TEXELS + SkinnedColor.VALUES.length
                + EngineSetting.SKINNED_APPEARANCE_DETAIL_TEXELS;
    }
}
