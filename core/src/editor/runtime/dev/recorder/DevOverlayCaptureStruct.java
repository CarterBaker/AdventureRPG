package editor.runtime.dev.recorder;

import java.nio.ByteBuffer;
import java.util.concurrent.Future;
import java.util.zip.Deflater;

import application.bootstrap.renderpipeline.fbo.FBOInstance;
import application.bootstrap.renderpipeline.pbo.PBOInstance;
import editor.bootstrap.recorderpipeline.recording.RecorderOverlay;
import editor.bootstrap.recorderpipeline.util.RecordingOverlayUtility;
import engine.root.EngineSetting;
import engine.root.StructPackage;

public class DevOverlayCaptureStruct extends StructPackage {

    /*
     * What capturing one overlay of a Dev window holds between frames: the
     * target it pushed last frame, the readback under way of what that target
     * drew and the take moment it belongs to, the checksum and size of the
     * last frame kept so a frame that changed nothing is never kept twice,
     * whether the overlay was last kept as shown, and the buffers and the
     * frame being packed on the encoding thread. The packing buffers belong
     * to that thread while a frame is packed.
     */

    // Identity
    private final RecorderOverlay overlay;

    // Read
    private FBOInstance pushed;
    private PBOInstance pbo;
    private ByteBuffer readback;
    private boolean readPending;
    private float readTime;
    private int readWidth;
    private int readHeight;

    // Kept
    private boolean shown;
    private long lastChecksum;
    private int width;
    private int height;
    private int framesSinceKeyframe;

    // Packing
    private byte[] pixels;
    private byte[] previous;
    private byte[] difference;
    private byte[] packed;
    private final Deflater deflater;

    // Job
    private Future<?> job;
    private byte[] jobResult;
    private float jobTime;
    private int jobWidth;
    private int jobHeight;
    private boolean jobKeyframe;

    // Constructor \\

    public DevOverlayCaptureStruct(RecorderOverlay overlay) {

        // Identity
        this.overlay = overlay;

        // Packing
        this.deflater = new Deflater(Deflater.BEST_SPEED);
    }

    // Read \\

    // A readback buffer able to hold a frame of the size, kept when it already can
    public void ensureReadback(int frameWidth, int frameHeight) {

        int length = frameWidth * frameHeight * EngineSetting.BYTES_PER_PIXEL_BGRA;

        if (readback == null || readback.capacity() < length)
            this.readback = ByteBuffer.allocateDirect(length);
    }

    public void queueRead(float time, int frameWidth, int frameHeight) {
        this.readPending = true;
        this.readTime = time;
        this.readWidth = frameWidth;
        this.readHeight = frameHeight;
    }

    public void clearRead() {
        this.readPending = false;
    }

    // Packing \\

    // Packing buffers for frames of the size, true when they had to be made anew so the next frame stands alone
    public boolean ensurePacking(int frameWidth, int frameHeight) {

        if (pixels != null && width == frameWidth && height == frameHeight)
            return false;

        int length = frameWidth * frameHeight * EngineSetting.BYTES_PER_PIXEL_BGRA;

        this.pixels = new byte[length];
        this.previous = new byte[length];
        this.difference = new byte[length];
        this.packed = new byte[RecordingOverlayUtility.resolvePackedCapacity(length)];
        this.width = frameWidth;
        this.height = frameHeight;
        return true;
    }

    public void keep(long checksum, boolean keyframe) {
        this.lastChecksum = checksum;
        this.shown = true;
        this.framesSinceKeyframe = keyframe ? 1 : framesSinceKeyframe + 1;
    }

    // The next frame kept stands alone, as after the overlay was hidden or a frame was lost
    public void hide() {
        this.shown = false;
    }

    // Job \\

    public void beginJob(Future<?> job, float time, int frameWidth, int frameHeight, boolean keyframe) {
        this.job = job;
        this.jobResult = null;
        this.jobTime = time;
        this.jobWidth = frameWidth;
        this.jobHeight = frameHeight;
        this.jobKeyframe = keyframe;
    }

    public void setJobResult(byte[] jobResult) {
        this.jobResult = jobResult;
    }

    public void clearJob() {
        this.job = null;
        this.jobResult = null;
    }

    // Release \\

    // Every buffer let go once nothing more is captured; the readback target stays with the window
    public void release() {
        this.pushed = null;
        this.readback = null;
        this.readPending = false;
        this.shown = false;
        this.pixels = null;
        this.previous = null;
        this.difference = null;
        this.packed = null;
        clearJob();
    }

    // Accessible \\

    public RecorderOverlay getOverlay() {
        return overlay;
    }

    public FBOInstance getPushed() {
        return pushed;
    }

    public void setPushed(FBOInstance pushed) {
        this.pushed = pushed;
    }

    public PBOInstance getPbo() {
        return pbo;
    }

    public void setPbo(PBOInstance pbo) {
        this.pbo = pbo;
    }

    public ByteBuffer getReadback() {
        return readback;
    }

    public boolean isReadPending() {
        return readPending;
    }

    public float getReadTime() {
        return readTime;
    }

    public int getReadWidth() {
        return readWidth;
    }

    public int getReadHeight() {
        return readHeight;
    }

    public boolean isShown() {
        return shown;
    }

    public long getLastChecksum() {
        return lastChecksum;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getFramesSinceKeyframe() {
        return framesSinceKeyframe;
    }

    public byte[] getPixels() {
        return pixels;
    }

    public byte[] getPrevious() {
        return previous;
    }

    public byte[] getDifference() {
        return difference;
    }

    public byte[] getPacked() {
        return packed;
    }

    public Deflater getDeflater() {
        return deflater;
    }

    public boolean hasJob() {
        return job != null;
    }

    public Future<?> getJob() {
        return job;
    }

    public byte[] getJobResult() {
        return jobResult;
    }

    public float getJobTime() {
        return jobTime;
    }

    public int getJobWidth() {
        return jobWidth;
    }

    public int getJobHeight() {
        return jobHeight;
    }

    public boolean isJobKeyframe() {
        return jobKeyframe;
    }
}
