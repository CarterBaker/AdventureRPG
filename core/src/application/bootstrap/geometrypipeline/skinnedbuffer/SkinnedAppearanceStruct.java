package application.bootstrap.geometrypipeline.skinnedbuffer;

import application.bootstrap.shaderpipeline.texture.TextureHandle;
import engine.graphics.color.Color;
import engine.root.EngineSetting;
import engine.root.StructPackage;
import engine.util.mathematics.vectors.Vector4;

public class SkinnedAppearanceStruct extends StructPackage {

    /*
     * Per-instance appearance row written beside the model matrix of every
     * skinned draw — SKINNED_INSTANCE_APPEARANCE_FLOATS floats, six vec4
     * attributes in this exact order: tint (rgb, w = hidden bone index),
     * detail tint (rgb, w unused), face region, eyes region, brows region,
     * mouth region. Regions are atlas-space tile rects (u0, v0, u1, v1); an
     * all-zero region disables that overlay in the shader. Reused as a
     * mutable scratch by whoever submits skinned draws — never stored.
     */

    // Tint
    private final Vector4 tint;
    private final Vector4 detailTint;

    // Regions
    private final Vector4 faceRegion;
    private final Vector4 eyesRegion;
    private final Vector4 browsRegion;
    private final Vector4 mouthRegion;

    // Constructor \\

    public SkinnedAppearanceStruct() {

        // Tint
        this.tint = new Vector4();
        this.detailTint = new Vector4();

        // Regions
        this.faceRegion = new Vector4();
        this.eyesRegion = new Vector4();
        this.browsRegion = new Vector4();
        this.mouthRegion = new Vector4();

        reset();
    }

    // Management \\

    public void reset() {

        tint.set(Color.WHITE.r, Color.WHITE.g, Color.WHITE.b, EngineSetting.SKINNED_HIDDEN_BONE_NONE);
        detailTint.set(Color.WHITE.r, Color.WHITE.g, Color.WHITE.b, Color.WHITE.a);

        faceRegion.set(0f);
        eyesRegion.set(0f);
        browsRegion.set(0f);
        mouthRegion.set(0f);
    }

    public void setTint(Color color) {
        tint.set(color.r, color.g, color.b, tint.w);
    }

    public void setHiddenBone(float hiddenBone) {
        tint.w = hiddenBone;
    }

    public void setDetailTint(Color color) {
        detailTint.set(color.r, color.g, color.b, color.a);
    }

    public void setFaceRegion(TextureHandle textureHandle) {
        setRegion(faceRegion, textureHandle);
    }

    public void setEyesRegion(TextureHandle textureHandle) {
        setRegion(eyesRegion, textureHandle);
    }

    public void setBrowsRegion(TextureHandle textureHandle) {
        setRegion(browsRegion, textureHandle);
    }

    public void setMouthRegion(TextureHandle textureHandle) {
        setRegion(mouthRegion, textureHandle);
    }

    // Write \\

    public void write(float[] out, int offset) {

        int cursor = offset;

        cursor = writeVector(out, cursor, tint);
        cursor = writeVector(out, cursor, detailTint);
        cursor = writeVector(out, cursor, faceRegion);
        cursor = writeVector(out, cursor, eyesRegion);
        cursor = writeVector(out, cursor, browsRegion);
        writeVector(out, cursor, mouthRegion);
    }

    // Utility \\

    private void setRegion(Vector4 region, TextureHandle textureHandle) {

        if (textureHandle == null) {
            region.set(0f);
            return;
        }

        region.set(textureHandle.getU0(), textureHandle.getV0(), textureHandle.getU1(), textureHandle.getV1());
    }

    private int writeVector(float[] out, int cursor, Vector4 vector) {

        out[cursor++] = vector.x;
        out[cursor++] = vector.y;
        out[cursor++] = vector.z;
        out[cursor++] = vector.w;

        return cursor;
    }
}
