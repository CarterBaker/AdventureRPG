package editor.runtime.viewcamera;

public interface ViewCameraSubject {

    /*
     * What a 3D viewport frames: whether anything is shown, the middle of it
     * on each axis and the radius of a sphere about that middle holding it,
     * in model blocks. The view camera turns about the middle and stands off
     * in radii, so every viewport frames what it shows the same way however
     * large it is. A subject with a surface, a globe, names its radius: the
     * camera then stands off that surface rather than the middle, so it can
     * close on the ground as near as a single pixel of it.
     */

    boolean isShown();

    float getCenter(int axis);

    float getBoundingRadius();

    // The radius about the middle of a surface the camera stays above, none for a subject it may close on to its middle
    default float getSurfaceRadius() {
        return 0f;
    }
}
