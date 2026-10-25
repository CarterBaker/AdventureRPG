package editor.bootstrap.recorderpipeline.recording;

import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.EngineSetting;
import engine.root.StructPackage;

public class RecordingDragStruct extends StructPackage {

    /*
     * The drag the Recorder tab has under way: what is carried, the index of
     * its take or clip and of the node pressed on, the window the press began
     * in, where and at what moment the press began so a press that never
     * travelled reads as a click, how far into the carried span the cursor
     * took hold, where the view started so a scroll can follow the cursor,
     * the key taken hold of and the value under the cursor when it was, and
     * where it would land now — its start and lane, and whether the cursor
     * is over the lanes at all.
     */

    // Carried
    private RecorderDragKind kind;
    private int index;
    private int node;
    private WindowInstance window;

    // Press
    private float pressX;
    private float pressY;
    private float pressTime;
    private float grabOffset;
    private float viewStart;
    private boolean moved;

    // Key
    private float keyTime;
    private double keyValue;
    private double pressValue;

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
            int node,
            WindowInstance window,
            float pressX,
            float pressY,
            float pressTime,
            float grabOffset,
            float viewStart) {

        // Carried
        this.kind = kind;
        this.index = index;
        this.node = node;
        this.window = window;

        // Press
        this.pressX = pressX;
        this.pressY = pressY;
        this.pressTime = pressTime;
        this.grabOffset = grabOffset;
        this.viewStart = viewStart;
        this.moved = false;

        // Landing
        this.overLanes = false;
    }

    // The key taken hold of as it stood, and the value under the cursor, so a drag moves it by how far it travels
    public void anchorKey(float keyTime, double keyValue, double pressValue) {
        this.keyTime = keyTime;
        this.keyValue = keyValue;
        this.pressValue = pressValue;
    }

    // The value under the cursor where a press on nothing on the graph began, so the graph can follow the cursor
    public void anchorValue(double pressValue) {
        this.pressValue = pressValue;
    }

    public void clear() {
        this.kind = RecorderDragKind.NONE;
        this.index = EngineSetting.INDEX_NOT_FOUND;
        this.node = EngineSetting.INDEX_NOT_FOUND;
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

    public int getNode() {
        return node;
    }

    public WindowInstance getWindow() {
        return window;
    }

    public float getPressX() {
        return pressX;
    }

    public float getPressTime() {
        return pressTime;
    }

    public float getGrabOffset() {
        return grabOffset;
    }

    public float getViewStart() {
        return viewStart;
    }

    public float getKeyTime() {
        return keyTime;
    }

    public double getKeyValue() {
        return keyValue;
    }

    public double getPressValue() {
        return pressValue;
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
