package editor.runtime.dev.recorder;

import java.nio.ByteBuffer;
import java.util.concurrent.Future;
import java.util.zip.Inflater;

import application.bootstrap.renderpipeline.fbo.FBOInstance;
import editor.bootstrap.recorderpipeline.recording.RecorderOverlay;
import editor.bootstrap.recorderpipeline.recording.RecordingOverlayStreamStruct;
import engine.root.EngineSetting;
import engine.root.StructPackage;

public class DevOverlayPlaybackStruct extends StructPackage {

    /*
     * What playing one overlay back in a Dev window holds between frames:
     * the target made from the overlay's own definition that the frames are
     * written into, the frame wanted at the playhead, the frame last unpacked
     * and the size it was written at, the frame being unpacked on the
     * encoding thread, and a frame that could not be read so it is never
     * tried again. The unpacking buffers and the upload buffer belong to that
     * thread while a frame is unpacked.
     */

    // Identity
    private final RecorderOverlay overlay;

    // Target
    private FBOInstance fbo;

    // Wanted
    private RecordingOverlayStreamStruct wantedStream;
    private int wantedFrame;

    // Decoded
    private RecordingOverlayStreamStruct decodedStream;
    private int decodedFrame;
    private int decodedWidth;
    private int decodedHeight;

    // Refused
    private RecordingOverlayStreamStruct refusedStream;
    private int refusedFrame;

    // Unpacking
    private byte[] pixels;
    private byte[] difference;
    private ByteBuffer upload;
    private final Inflater inflater;

    // Job
    private Future<?> job;
    private RecordingOverlayStreamStruct jobStream;
    private int jobFrame;
    private boolean jobFailed;

    // Constructor \\

    public DevOverlayPlaybackStruct(RecorderOverlay overlay) {

        // Identity
        this.overlay = overlay;

        // Unpacking
        this.inflater = new Inflater();

        clearWanted();
        clearDecoded();
        this.refusedFrame = EngineSetting.INDEX_NOT_FOUND;
    }

    // Wanted \\

    public void want(RecordingOverlayStreamStruct stream, int frame) {
        this.wantedStream = stream;
        this.wantedFrame = frame;
    }

    public void clearWanted() {
        this.wantedStream = null;
        this.wantedFrame = EngineSetting.INDEX_NOT_FOUND;
    }

    public boolean isWanting() {
        return wantedStream != null;
    }

    // True when the frame wanted is the one written into the target
    public boolean isSettled() {
        return job == null && (wantedStream == null || isDecoded(wantedStream, wantedFrame)
                || isRefused(wantedStream, wantedFrame));
    }

    // Decoded \\

    public boolean isDecoded(RecordingOverlayStreamStruct stream, int frame) {
        return decodedStream == stream && decodedFrame == frame;
    }

    public void setDecoded(RecordingOverlayStreamStruct stream, int frame, int width, int height) {
        this.decodedStream = stream;
        this.decodedFrame = frame;
        this.decodedWidth = width;
        this.decodedHeight = height;
    }

    public void clearDecoded() {
        this.decodedStream = null;
        this.decodedFrame = EngineSetting.INDEX_NOT_FOUND;
    }

    public boolean hasDecoded() {
        return decodedStream != null;
    }

    // Refused \\

    public boolean isRefused(RecordingOverlayStreamStruct stream, int frame) {
        return refusedStream == stream && refusedFrame == frame;
    }

    public void refuse(RecordingOverlayStreamStruct stream, int frame) {
        this.refusedStream = stream;
        this.refusedFrame = frame;
    }

    // Unpacking \\

    // Unpacking buffers able to hold a frame of the size, kept when they already can
    public void ensureUnpacking(int width, int height) {

        int length = width * height * EngineSetting.BYTES_PER_PIXEL_BGRA;

        if (pixels != null && pixels.length == length)
            return;

        this.pixels = new byte[length];
        this.difference = new byte[length];
        this.upload = ByteBuffer.allocateDirect(length);
        clearDecoded();
    }

    // Job \\

    public void beginJob(Future<?> job, RecordingOverlayStreamStruct stream, int frame) {
        this.job = job;
        this.jobStream = stream;
        this.jobFrame = frame;
        this.jobFailed = false;
    }

    public void failJob() {
        this.jobFailed = true;
    }

    public void clearJob() {
        this.job = null;
        this.jobStream = null;
    }

    // Release \\

    // Every buffer let go once nothing more plays; the target stays with the window
    public void release() {
        clearWanted();
        clearDecoded();
        this.refusedStream = null;
        this.pixels = null;
        this.difference = null;
        this.upload = null;
        clearJob();
    }

    // Accessible \\

    public RecorderOverlay getOverlay() {
        return overlay;
    }

    public FBOInstance getFbo() {
        return fbo;
    }

    public void setFbo(FBOInstance fbo) {
        this.fbo = fbo;
    }

    public RecordingOverlayStreamStruct getWantedStream() {
        return wantedStream;
    }

    public int getWantedFrame() {
        return wantedFrame;
    }

    public RecordingOverlayStreamStruct getDecodedStream() {
        return decodedStream;
    }

    public int getDecodedFrame() {
        return decodedFrame;
    }

    public int getDecodedWidth() {
        return decodedWidth;
    }

    public int getDecodedHeight() {
        return decodedHeight;
    }

    public byte[] getPixels() {
        return pixels;
    }

    public byte[] getDifference() {
        return difference;
    }

    public ByteBuffer getUpload() {
        return upload;
    }

    public Inflater getInflater() {
        return inflater;
    }

    public boolean hasJob() {
        return job != null;
    }

    public Future<?> getJob() {
        return job;
    }

    public RecordingOverlayStreamStruct getJobStream() {
        return jobStream;
    }

    public int getJobFrame() {
        return jobFrame;
    }

    public boolean isJobFailed() {
        return jobFailed;
    }
}
