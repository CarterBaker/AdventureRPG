package editor.runtime.viewcamera;

public interface ViewCameraSubject {

    /*
     * What a 3D viewport frames: whether anything is shown, the middle of it
     * on each axis and the radius of a sphere about that middle holding it,
     * in model blocks. The view camera turns about the middle and stands off
     * in radii, so every viewport frames what it shows the same way however
     * large it is.
     */

    boolean isShown();

    float getCenter(int axis);

    float getBoundingRadius();
}
