package application.bootstrap.animationpipeline.animation;

import engine.root.StructPackage;
import engine.util.mathematics.vectors.Vector3;

public class AnimationKeyframeStruct extends StructPackage {

    /*
     * Immutable sample point on a single bone's track. rotation is Euler
     * degrees (XYZ order) applied on top of the rig's bind-pose rotation.
     * position is an additive offset on top of the bind-pose position —
     * zero for the overwhelming majority of keyframes. scale is a
     * multiplier on top of 1.0, per axis — used only for stretch/squash
     * style tracks. Keyframes within one BoneTrackStruct are supplied in
     * strictly increasing time order — enforced by InternalBuilder, never
     * re-checked at runtime.
     */

    // Timing
    private final float time;

    // Pose
    private final Vector3 rotation;
    private final Vector3 position;
    private final Vector3 scale;

    // Constructor \\

    public AnimationKeyframeStruct(
            float time,
            Vector3 rotation,
            Vector3 position,
            Vector3 scale) {

        // Timing
        this.time = time;

        // Pose
        this.rotation = rotation;
        this.position = position;
        this.scale = scale;
    }

    // Accessible \\

    public float getTime() {
        return time;
    }

    public Vector3 getRotation() {
        return rotation;
    }

    public Vector3 getPosition() {
        return position;
    }

    public Vector3 getScale() {
        return scale;
    }
}