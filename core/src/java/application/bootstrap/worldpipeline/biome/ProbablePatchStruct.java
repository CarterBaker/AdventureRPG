package application.bootstrap.worldpipeline.biome;

import engine.root.EngineSetting;
import engine.root.StructPackage;

public class ProbablePatchStruct extends StructPackage {

    /*
     * What one probable biome resolves to at a position: the share it covers
     * and the patch covering most of it, as its hash, outline metric (below 1
     * inside), edge noise, the position in the patch's own shape space and
     * the world position of its center, so a biome placed at its center can
     * follow its outline and a lake can read its level at its heart. Crest is
     * how far a relative biome's offset holds at the position, blended over
     * every patch reaching it: 1 at a dome's heart or wherever the outline
     * breaks sharp, falling to 0 at a rolling outline. Where no discrete
     * patch covers, the outline is NO_PATCH_SHAPE and the crest is full.
     */

    public static final float NO_PATCH_SHAPE = EngineSetting.BIOME_PROBABLE_NO_PATCH_SHAPE;

    private float coverage;
    private long patchHash;
    private float patchShape;
    private float patchNoise;
    private double patchX;
    private double patchZ;
    private double centerX;
    private double centerZ;
    private float crest;

    // Management \\

    public void reset() {
        this.coverage = 0f;
        this.patchHash = 0L;
        this.patchShape = NO_PATCH_SHAPE;
        this.patchNoise = 0f;
        this.patchX = 0.0;
        this.patchZ = 0.0;
        this.centerX = 0.0;
        this.centerZ = 0.0;
        this.crest = 1f;
    }

    public void set(
            float coverage,
            long patchHash,
            float patchShape,
            float patchNoise,
            double patchX,
            double patchZ,
            double centerX,
            double centerZ) {

        this.coverage = coverage;
        this.patchHash = patchHash;
        this.patchShape = patchShape;
        this.patchNoise = patchNoise;
        this.patchX = patchX;
        this.patchZ = patchZ;
        this.centerX = centerX;
        this.centerZ = centerZ;
    }

    public void setCoverage(float coverage) {
        this.coverage = coverage;
    }

    public void setCrest(float crest) {
        this.crest = crest;
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

    public double getPatchX() {
        return patchX;
    }

    public double getPatchZ() {
        return patchZ;
    }

    public double getCenterX() {
        return centerX;
    }

    public double getCenterZ() {
        return centerZ;
    }

    public float getCrest() {
        return crest;
    }

    public boolean hasPatch() {
        return patchShape != NO_PATCH_SHAPE;
    }
}
