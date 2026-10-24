package editor.bootstrap.recorderpipeline.recording;

import editor.runtime.EditorSetting;
import engine.root.StructPackage;

public class RecordingExportStruct extends StructPackage {

    /*
     * How a recording renders to video: the output size, the frames per
     * second, the file format, how many moments blend into each frame for
     * motion blur, whether each frame is rendered at twice the size and
     * shrunk for clean edges, and the span of the timeline exported — the
     * whole recording while no range is set.
     */

    // Output
    private int width;
    private int height;
    private int frameRate;
    private VideoFormat format;

    // Quality
    private int motionBlurSamples;
    private boolean supersampled;

    // Range
    private float rangeStart;
    private float rangeEnd;

    // Constructor \\

    public RecordingExportStruct() {

        // Output
        this.width = EditorSetting.RECORDER_EXPORT_DEFAULT_WIDTH;
        this.height = EditorSetting.RECORDER_EXPORT_DEFAULT_HEIGHT;
        this.frameRate = EditorSetting.RECORDER_EXPORT_DEFAULT_FRAME_RATE;
        this.format = VideoFormat.MP4;

        // Quality
        this.motionBlurSamples = EditorSetting.RECORDER_EXPORT_DEFAULT_MOTION_BLUR_SAMPLES;

        // Range
        clearRange();
    }

    // Range \\

    public void clearRange() {
        this.rangeStart = EditorSetting.RECORDER_TIME_UNSET;
        this.rangeEnd = EditorSetting.RECORDER_TIME_UNSET;
    }

    public boolean hasRangeStart() {
        return rangeStart >= 0f;
    }

    public boolean hasRangeEnd() {
        return rangeEnd >= 0f;
    }

    // Where the export starts on a recording of this length
    public float resolveStart(float duration) {
        return hasRangeStart() ? Math.min(rangeStart, duration) : 0f;
    }

    // Where the export ends on a recording of this length, never before it starts
    public float resolveEnd(float duration) {
        return Math.max(resolveStart(duration), hasRangeEnd() ? Math.min(rangeEnd, duration) : duration);
    }

    // Accessible \\

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int getFrameRate() {
        return frameRate;
    }

    public void setFrameRate(int frameRate) {
        this.frameRate = frameRate;
    }

    public VideoFormat getFormat() {
        return format;
    }

    public void setFormat(VideoFormat format) {
        this.format = format;
    }

    public int getMotionBlurSamples() {
        return motionBlurSamples;
    }

    public void setMotionBlurSamples(int motionBlurSamples) {
        this.motionBlurSamples = motionBlurSamples;
    }

    public boolean isSupersampled() {
        return supersampled;
    }

    public void setSupersampled(boolean supersampled) {
        this.supersampled = supersampled;
    }

    public float getRangeStart() {
        return rangeStart;
    }

    public void setRangeStart(float rangeStart) {
        this.rangeStart = rangeStart;
    }

    public float getRangeEnd() {
        return rangeEnd;
    }

    public void setRangeEnd(float rangeEnd) {
        this.rangeEnd = rangeEnd;
    }
}
