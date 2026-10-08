package application.bootstrap.worldpipeline.biome;

import engine.root.EngineSetting;
import engine.root.StructPackage;

public class ProbablePatchStruct extends StructPackage {

    /*
     * What one probable biome resolves to at a position: the share it covers
     * and the patch covering most of it, as its hash, outline metric (below 1
     * inside) and edge noise, so a biome placed at its center can follow its
     * outline. Where no discrete patch covers, the outline is NO_PATCH_SHAPE.
     */

    public static final float NO_PATCH_SHAPE = EngineSetting.BIOME_PROBABLE_NO_PATCH_SHAPE;

    private float coverage;
    private long patchHash;
    private float patchShape;
    private float patchNoise;

    // Management \\

    public void reset() {
        this.coverage = 0f;
        this.patchHash = 0L;
        this.patchShape = NO_PATCH_SHAPE;
        this.patchNoise = 0f;
    }

    public void set(float coverage, long patchHash, float patchShape, float patchNoise) {
        this.coverage = coverage;
        this.patchHash = patchHash;
        this.patchShape = patchShape;
        this.patchNoise = patchNoise;
    }

    public void setCoverage(float coverage) {
        this.coverage = coverage;
    }

    // Accessible \\

    public float getCoverage() {
        return coverage;
    }

    public long getPatchHash() {
        return patchHash;
    }

    public float getPatchShape() {
        return patchShape;
    }

    public float getPatchNoise() {
        return patchNoise;
    }
}
