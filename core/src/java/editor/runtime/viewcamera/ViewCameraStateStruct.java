package editor.runtime.viewcamera;

import editor.runtime.EditorSetting;
import engine.root.StructPackage;
import engine.util.mathematics.vectors.Vector3;

public class ViewCameraStateStruct extends StructPackage {

    /*
     * The view a camera looks through: the way it faces, how far the point it
     * turns about is slid from the middle of what is shown, and how far it
     * stands off in bounding radii. Every view camera keeps its own, and a
     * view copied into another camera's frames the same part of what that
     * camera shows, so stacked tabs hand their view on by copying it.
     */

    // Orbit
    private final Vector3 direction;
    private final Vector3 pan;
    private float distance;

    // Constructor \\

    public ViewCameraStateStruct() {

        // Orbit
        this.direction = new Vector3();
        this.pan = new Vector3();
        this.distance = EditorSetting.VIEW_CAMERA_DISTANCE_DEFAULT;
    }

    // Orbit \\

    // The view faced along a direction, kept at unit length
    public void aim(Vector3 facing) {
        direction.set(facing).normalize();
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    // This view made the same as another
    public void copy(ViewCameraStateStruct source) {
        direction.set(source.direction);
        pan.set(source.pan);
        this.distance = source.distance;
    }

    // Accessible \\

    // The way the view faces, in model space
    public Vector3 getDirection() {
        return direction;
    }

    // How far the point the view turns about is slid from the middle of what is shown, in model blocks
    public Vector3 getPan() {
        return pan;
    }

    // How far the camera stands off, in bounding radii of what is shown
    public float getDistance() {
        return distance;
    }
}
