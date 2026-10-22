package editor.bootstrap.recorderpipeline.recording;

import editor.bootstrap.recorderpipeline.util.RecordingSampleUtility;
import engine.root.EngineSetting;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class RecordingOverlayStreamStruct extends StructPackage {

    /*
     * One overlay through a take, frame by frame, a frame kept only when what
     * it shows changed: when it began, its size in pixels, whether it stands
     * alone or holds only its difference from the frame before, and its
     * compressed pixels. A frame of no pixels means the overlay was not drawn
     * from then on. A frame that holds a difference is read from the last
     * frame that stands alone, forward.
     */

    // Identity
    private final RecorderOverlay overlay;

    // Frames
    private final FloatArrayList times;
    private final IntArrayList widths;
    private final IntArrayList heights;
    private final IntArrayList keyframes;
    private final ObjectArrayList<byte[]> frames;

    // Constructor \\

    public RecordingOverlayStreamStruct(RecorderOverlay overlay) {

        // Identity
        this.overlay = overlay;

        // Frames
        this.times = new FloatArrayList();
        this.widths = new IntArrayList();
        this.heights = new IntArrayList();
        this.keyframes = new IntArrayList();
        this.frames = new ObjectArrayList<>();
    }

    // Management \\

    public void addFrame(float time, int width, int height, boolean keyframe, byte[] frame) {
        times.add(time);
        widths.add(width);
        heights.add(height);
        keyframes.add(keyframe ? 1 : 0);
        frames.add(frame);
    }

    public void shiftTimes(float seconds) {
        RecordingSampleUtility.shiftTimes(times, seconds);
    }

    // The frame showing at the time, -1 before the first
    public int findFrame(float time) {
        return RecordingSampleUtility.findSample(times, time);
    }

    // The last frame at or before this one that stands alone, -1 when there is none
    public int findKeyframe(int frame) {

        for (int i = frame; i >= 0; i--)
            if (isKeyframe(i))
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    public boolean isHidden(int frame) {
        return frames.get(frame).length == 0;
    }

    // Accessible \\

    public RecorderOverlay getOverlay() {
        return overlay;
    }

    public int size() {
        return times.size();
    }

    public boolean isEmpty() {
        return times.isEmpty();
    }

    public float getTime(int frame) {
        return times.getFloat(frame);
    }

    public int getWidth(int frame) {
        return widths.getInt(frame);
    }

    public int getHeight(int frame) {
        return heights.getInt(frame);
    }

    public boolean isKeyframe(int frame) {
        return keyframes.getInt(frame) != 0;
    }

    public byte[] getFrame(int frame) {
        return frames.get(frame);
    }

    public FloatArrayList getTimes() {
        return times;
    }

    public IntArrayList getWidths() {
        return widths;
    }

    public IntArrayList getHeights() {
        return heights;
    }

    public IntArrayList getKeyframes() {
        return keyframes;
    }

    public ObjectArrayList<byte[]> getFrames() {
        return frames;
    }

    // Every compressed byte the stream holds
    public long getByteCount() {

        long byteCount = 0L;

        for (int i = 0; i < frames.size(); i++)
            byteCount += frames.get(i).length;

        return byteCount;
    }
}
