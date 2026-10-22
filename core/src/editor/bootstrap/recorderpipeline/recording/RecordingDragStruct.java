package editor.bootstrap.recorderpipeline.recording;

import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.EngineSetting;
import engine.root.StructPackage;

public class RecordingDragStruct extends StructPackage {

    /*
     * The drag the Recorder tab has under way: what is carried and its index,
     * the window the press began in, where the press began so a press that
     * never travelled reads as a click, how far into the carried span the
     * cursor took hold, and where it would land now — its start and lane,
     * and whether the cursor is over the lanes at all.
     */

    // Carried
    private RecorderDragKind kind;
    private int index;
    private WindowInstance window;

    // Press
    private float pressX;
    private float pressY;
    private float grabOffset;
    private boolean moved;

    // Landing
    private boolean overLanes;
    private float landingStart;
    private int landingLane;

    // Constructor \\

    public RecordingDragStruct() {
        clear();
    }

    // Management \\

    public void begin(
            RecorderDragKind kind,
            int index,
            WindowInstance window,
            float pressX,
            float pressY,
            float grabOffset) {

        // Carried
        this.kind = kind;
        this.index = index;
        this.window = window;

        // Press
        this.pressX = pressX;
        this.pressY = pressY;
        this.grabOffset = grabOffset;
        this.moved = false;

        // Landing
        this.overLanes = false;
    }

    public void clear() {
        this.kind = RecorderDragKind.NONE;
        this.index = EngineSetting.INDEX_NOT_FOUND;
        this.window = null;
        this.overLanes = false;
        this.moved = false;
    }

    // Travel past the threshold from where the press began makes it a drag for good
    public void travel(float x, float y, float threshold) {
        if (Math.abs(x - pressX) > threshold || Math.abs(y - pressY) > threshold)
            this.moved = true;
    }

    public void land(boolean overLanes, float landingStart, int landingLane) {
        this.overLanes = overLanes;
        this.landingStart = landingStart;
        this.landingLane = landingLane;
    }

    // Accessible \\

    public boolean isActive() {
        return kind != RecorderDragKind.NONE;
    }

    public RecorderDragKind getKind() {
        return kind;
    }

    public int getIndex() {
        return index;
    }

    public WindowInstance getWindow() {
        return window;
    }

    public float getGrabOffset() {
        return grabOffset;
    }

    public boolean isMoved() {
        return moved;
    }

    public boolean isOverLanes() {
        return overLanes;
    }

    public float getLandingStart() {
        return landingStart;
    }

    public int getLandingLane() {
        return landingLane;
    }
}
