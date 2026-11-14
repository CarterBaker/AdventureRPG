package editor.bootstrap.charactereditorpipeline.charactermesh;

import engine.root.StructPackage;
import engine.util.mathematics.vectors.Vector3;

public class CharacterStampStruct extends StructPackage {

    /*
     * Where one stamp of a brush lands on a character mesh, in the mesh's
     * own space: the point under the brush's centre, the brush's radius at
     * that depth, the camera's line of sight, how far the pointer carried the
     * brush since the last stamp, and whether the stroke is inverted. The
     * viewport owns one and fills it before every stamp, so a stroke never
     * allocates.
     */

    // Placement
    private final Vector3 center;
    private final Vector3 viewDirection;
    private final Vector3 drag;
    private float radius;

    // Direction
    private boolean inverted;

    // Constructor \\

    public CharacterStampStruct() {

        // Placement
        this.center = new Vector3();
        this.viewDirection = new Vector3();
        this.drag = new Vector3();
    }

    // Management \\

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public void setInverted(boolean inverted) {
        this.inverted = inverted;
    }

    // Accessible \\

    public Vector3 getCenter() {
        return center;
    }

    public Vector3 getViewDirection() {
        return viewDirection;
    }

    public Vector3 getDrag() {
        return drag;
    }

    public float getRadius() {
        return radius;
    }

    public boolean isInverted() {
        return inverted;
    }

    // One for a stroke as drawn, minus one for an inverted stroke
    public float getDirection() {
        return inverted ? -1f : 1f;
    }
}
