package application.bootstrap.menupipeline.element;

import engine.root.StructPackage;

public class ElementPoseStruct extends StructPackage {

    /*
     * Mutable scratch pose written by ElementAnimationStruct.sample() and read
     * by ElementInstance.applyPose(). MenuRenderSystem owns a single instance
     * and reuses it for every animated element each frame.
     */

    // Pose
    private float offsetX;
    private float offsetY;
    private float scaleX;
    private float scaleY;
    private float rotation;
    private float alpha;

    // Set \\

    public void set(ElementKeyframeStruct keyframe) {
        this.offsetX = keyframe.getOffsetX();
        this.offsetY = keyframe.getOffsetY();
        this.scaleX = keyframe.getScaleX();
        this.scaleY = keyframe.getScaleY();
        this.rotation = keyframe.getRotation();
        this.alpha = keyframe.getAlpha();
    }

    public void blend(ElementKeyframeStruct from, ElementKeyframeStruct to, float t) {
        this.offsetX = lerp(from.getOffsetX(), to.getOffsetX(), t);
        this.offsetY = lerp(from.getOffsetY(), to.getOffsetY(), t);
        this.scaleX = lerp(from.getScaleX(), to.getScaleX(), t);
        this.scaleY = lerp(from.getScaleY(), to.getScaleY(), t);
        this.rotation = lerp(from.getRotation(), to.getRotation(), t);

        // Overshooting eases may carry alpha past its range — premultiplied blending needs [0, 1]
        this.alpha = Math.max(0f, Math.min(1f, lerp(from.getAlpha(), to.getAlpha(), t)));
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    // Accessible \\

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
