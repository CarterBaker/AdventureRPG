package application.bootstrap.worldpipeline.util;

import application.bootstrap.worldpipeline.world.GlobeFrameStruct;
import application.bootstrap.worldpipeline.world.WorldHandle;
import engine.root.EngineSetting;
import engine.root.StructPackage;

public class FrameTransformStruct extends StructPackage {

    /*
     * Carries positions and headings from one flat frame of a world into
     * another, worked out once at a reference place and exact for everything
     * laid around it: a quarter-turn and an offset. On a torus the target is
     * the copy of the source nearest the target's reference. On a globe a
     * frame is a cube face's coordinates run on past its edges, or a grid's
     * chart, and the source reference is folded onto the face it truly lies
     * on and unfolded from there into the target face's frame, the axes
     * turning with it; a place on the face opposite the target's lies beyond
     * every reach and is held a stride off.
     */

    // Map
    private double sourceX;
    private double sourceZ;
    private double targetX;
    private double targetZ;
    private int turns;

    // Measure
    private double deltaX;
    private double deltaZ;

    // Fold
    private final GlobeFrameStruct fold = new GlobeFrameStruct();

    // Frames \\

    // From the frame a source reference is measured in to the frame a target reference is
    public void setFrames(
            WorldHandle worldHandle,
            double sourceReferenceX,
            double sourceReferenceZ,
            double targetReferenceX,
            double targetReferenceZ) {

        this.sourceX = sourceReferenceX;
        this.sourceZ = sourceReferenceZ;

        if (!worldHandle.isGlobe()) {
            this.targetX = WorldWrapUtility.unwrapBlockX(worldHandle, sourceReferenceX, targetReferenceX);
            this.targetZ = WorldWrapUtility.unwrapBlockZ(worldHandle, sourceReferenceZ, targetReferenceZ);
            this.turns = 0;
            return;
        }

        GlobeUtility.foldBlock(worldHandle, sourceReferenceX, sourceReferenceZ, fold);

        int sourceToTrue = fold.getTurns();

        layIntoTarget(
                worldHandle,
                GlobeUtility.toFoldedX(worldHandle, fold),
                GlobeUtility.toFoldedZ(worldHandle, fold),
                sourceToTrue,
                targetReferenceX);
    }

    // From a home chunk's chart to the frame a target reference is measured in
    public void setChartToFrame(
            SurfacePointStruct home,
            double chartX,
            double chartZ,
            double targetReferenceX,
            double targetReferenceZ) {

        if (!home.isGlobe()) {
            setFrames(home.getWorldHandle(), chartX, chartZ, targetReferenceX, targetReferenceZ);
            return;
        }

        this.sourceX = chartX;
        this.sourceZ = chartZ;

        home.resolve(chartX, chartZ);
        layIntoTarget(
                home.getWorldHandle(), home.getCanonicalX(), home.getCanonicalZ(), home.getTurns(), targetReferenceX);
    }

    // A canonical place the source reference truly stands at, unfolded into the target face's frame
    private void layIntoTarget(
            WorldHandle worldHandle,
            double canonicalX,
            double canonicalZ,
            int sourceToTrue,
            double targetReferenceX) {

        int faceBlocks = GlobeUtility.getFaceBlocks(worldHandle);
        int targetFace = GlobeUtility.faceOf(targetReferenceX, faceBlocks);

        GlobeUtility.unfoldBlock(worldHandle, targetFace, canonicalX, canonicalZ, fold);

        if (fold.getFace() != targetFace) {
            this.targetX = targetReferenceX + (double) faceBlocks * EngineSetting.GLOBE_FACE_STRIDE_SPANS;
            this.targetZ = canonicalZ;
            this.turns = 0;
            return;
        }

        this.targetX = GlobeUtility.faceOriginX(targetFace, faceBlocks) + (fold.getFoldA() + faceBlocks) * 0.5;
        this.targetZ = (fold.getFoldB() + faceBlocks) * 0.5;

        GlobeUtility.foldBlock(worldHandle, targetX, targetZ, fold);

        this.turns = sourceToTrue - fold.getTurns();
    }

    // Measure \\

    // How far a place lies from a reference, measured in the reference's frame: the torus's own wrapped difference,
    // or on a globe the place carried into the reference face's frame
    public void measure(WorldHandle worldHandle, double x, double z, double referenceX, double referenceZ) {

        if (!worldHandle.isGlobe()) {
            this.deltaX = WorldWrapUtility.wrappedBlockDeltaX(worldHandle, x, referenceX);
            this.deltaZ = WorldWrapUtility.wrappedBlockDeltaZ(worldHandle, z, referenceZ);
            return;
        }

        setFrames(worldHandle, x, z, referenceX, referenceZ);

        this.deltaX = targetX - referenceX;
        this.deltaZ = targetZ - referenceZ;
    }

    public double getDeltaX() {
        return deltaX;
    }

    public double getDeltaZ() {
        return deltaZ;
    }

    // Map \\

    public double mapX(double x, double z) {
        return targetX + GlobeUtility.turnX(x - sourceX, z - sourceZ, turns);
    }

    public double mapZ(double x, double z) {
        return targetZ + GlobeUtility.turnZ(x - sourceX, z - sourceZ, turns);
    }

    // A target place carried back into the source frame
    public double unmapX(double x, double z) {
        return sourceX + GlobeUtility.turnX(x - targetX, z - targetZ, -turns);
    }

    public double unmapZ(double x, double z) {
        return sourceZ + GlobeUtility.turnZ(x - targetX, z - targetZ, -turns);
    }

    // A heading or offset carried from the source frame's axes into the target's
    public double turnX(double x, double z) {
        return GlobeUtility.turnX(x, z, turns);
    }

    public double turnZ(double x, double z) {
        return GlobeUtility.turnZ(x, z, turns);
    }

    // Accessible \\

    // Quarter turns, each taking +X to +Z, from the source frame's axes into the target's
    public int getTurns() {
        return turns;
    }
}
