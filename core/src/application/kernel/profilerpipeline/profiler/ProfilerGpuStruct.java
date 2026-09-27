package application.kernel.profilerpipeline.profiler;

import engine.root.StructPackage;

public class ProfilerGpuStruct extends StructPackage {

    /*
     * The GPU record of one labelled scope, a shader batch, a render target
     * pass or a whole window frame: the GPU milliseconds its timestamps
     * measured, and the draw calls and triangles submitted inside it on the
     * CPU. Times arrive a few frames late, once the GPU has finished them.
     */

    // Identity
    private final String label;

    // Samples
    private final ProfilerSampleStruct millis;
    private final ProfilerSampleStruct drawCalls;
    private final ProfilerSampleStruct triangles;

    // Constructor \\

    public ProfilerGpuStruct(String label, int historyFrames) {

        // Identity
        this.label = label;

        // Samples
        this.millis = new ProfilerSampleStruct(label, historyFrames);
        this.drawCalls = new ProfilerSampleStruct(label, historyFrames);
        this.triangles = new ProfilerSampleStruct(label, historyFrames);
    }

    // Frame \\

    public void commit(long frameIndex, boolean capturing) {
        millis.commit(frameIndex, capturing);
        drawCalls.commit(frameIndex, capturing);
        triangles.commit(frameIndex, capturing);
    }

    public void resetCapture() {
        millis.resetCapture();
        drawCalls.resetCapture();
        triangles.resetCapture();
    }

    // Accessible \\

    public String getLabel() {
        return label;
    }

    public ProfilerSampleStruct getMillis() {
        return millis;
    }

    public ProfilerSampleStruct getDrawCalls() {
        return drawCalls;
    }

    public ProfilerSampleStruct getTriangles() {
        return triangles;
    }
}
