package application.bootstrap.menupipeline.element;

import application.bootstrap.menupipeline.util.MenuEase;
import engine.root.StructPackage;

public class ElementKeyframeStruct extends StructPackage {

    /*
     * One immutable pose in an element animation. Time is in seconds from the
     * start of the animation. Offsets are fractions of the element's own laid
     * out size, rotation is in degrees, and ease shapes the segment arriving at
     * this keyframe from the one before it.
     */

    // Timing
    private final float time;
    private final MenuEase ease;

    // Pose
    private final float offsetX;
    private final float offsetY;
    private final float scaleX;
    private final float scaleY;
    private final float rotation;
    private final float alpha;

    // Constructor \\

    public ElementKeyframeStruct(
            float time,
            MenuEase ease,
            float offsetX,
            float offsetY,
            float scaleX,
            float scaleY,
            float rotation,
            float alpha) {

        this.time = time;
        this.ease = ease;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.rotation = rotation;
        this.alpha = alpha;
    }

    // Accessible \\

    public float getTime() {
        return time;
    }

    public MenuEase getEase() {
        return ease;
    }

    public float getOffsetX() {
        return offsetX;
    }

    public float getOffsetY() {
        return offsetY;
    }

    public float getScaleX() {
        return scaleX;
    }

    public float getScaleY() {
        return scaleY;
    }

    public float getRotation() {
        return rotation;
    }

    public float getAlpha() {
        return alpha;
    }
}
