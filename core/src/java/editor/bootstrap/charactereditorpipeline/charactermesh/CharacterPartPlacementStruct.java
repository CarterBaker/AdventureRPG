package editor.bootstrap.charactereditorpipeline.charactermesh;

import editor.bootstrap.stamppipeline.stamp.StampHandle;
import engine.root.EngineSetting;
import engine.root.StructPackage;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector3;

public class CharacterPartPlacementStruct extends StructPackage {

    /*
     * Where one body part would join the character: the stamp, the point on
     * the body it joins at and the way the surface faces there, the hole's
     * radius, the bone its top bones hang from and the side its bones are
     * named for, whether it is mirrored across the body, and the matrix
     * carrying the stamp's space into the bind pose, its turn and scale kept
     * apart for the bones. A part joins either an open loop already in the
     * body or a hole cut where it is aimed; a root part starts the body over.
     * The refusal says why a placement cannot join, null when it can. One
     * placement is kept per viewport and filled again as the pointer moves.
     */

    // Stamp
    private StampHandle stampHandle;

    // Target
    private final Vector3 point;
    private final Vector3 normal;
    private float radius;
    private String hostBoneName;
    private String side;
    private boolean onLoop;
    private boolean root;
    private int attachPoint;

    // Transform — stamp space to the bind pose, and its turn alone
    private final Matrix4 transform;
    private final Matrix4 rotation;
    private float scale;
    private boolean mirrored;

    // State
    private String refusal;
    private boolean shown;

    // Constructor \\

    public CharacterPartPlacementStruct() {

        // Target
        this.point = new Vector3();
        this.normal = new Vector3();
        this.side = "";
        this.attachPoint = EngineSetting.INDEX_NOT_FOUND;

        // Transform
        this.transform = new Matrix4();
        this.rotation = new Matrix4();
        this.scale = 1f;
    }

    // Management \\

    // Nothing shown: the pointer is off the body, or no part is chosen
    public void clear() {
        this.stampHandle = null;
        this.shown = false;
        this.refusal = null;
        this.onLoop = false;
        this.root = false;
        this.mirrored = false;
        this.hostBoneName = null;
        this.side = "";
        this.attachPoint = EngineSetting.INDEX_NOT_FOUND;
    }

    public void begin(StampHandle stampHandle, float scale) {
        clear();
        this.stampHandle = stampHandle;
        this.scale = scale;
        this.shown = true;
    }

    // Takes every value of another placement
    public void set(CharacterPartPlacementStruct other) {
        this.stampHandle = other.stampHandle;
        this.point.set(other.point);
        this.normal.set(other.normal);
        this.radius = other.radius;
        this.hostBoneName = other.hostBoneName;
        this.side = other.side;
        this.onLoop = other.onLoop;
        this.root = other.root;
        this.attachPoint = other.attachPoint;
        this.transform.set(other.transform);
        this.rotation.set(other.rotation);
        this.scale = other.scale;
        this.mirrored = other.mirrored;
        this.refusal = other.refusal;
        this.shown = other.shown;
    }

    public void setTarget(float x, float y, float z, float nx, float ny, float nz, float radius, boolean onLoop) {
        this.point.set(x, y, z);
        this.normal.set(nx, ny, nz);
        this.radius = radius;
        this.onLoop = onLoop;
    }

    public void setHost(String hostBoneName, String side, int attachPoint) {
        this.hostBoneName = hostBoneName;
        this.side = side;
        this.attachPoint = attachPoint;
    }

    public void setRoot(boolean root) {
        this.root = root;
    }

    public void setMirrored(boolean mirrored) {
        this.mirrored = mirrored;
    }

    // The placement refused for a reason, the first given kept
    public void refuse(String refusal) {

        if (this.refusal == null)
            this.refusal = refusal;
    }

    // Accessible \\

    public StampHandle getStampHandle() {
        return stampHandle;
    }

    public Vector3 getPoint() {
        return point;
    }

    public Vector3 getNormal() {
        return normal;
    }

    public float getRadius() {
        return radius;
    }

    public String getHostBoneName() {
        return hostBoneName;
    }

    public String getSide() {
        return side;
    }

    public boolean isOnLoop() {
        return onLoop;
    }

    public boolean isRoot() {
        return root;
    }

    public int getAttachPoint() {
        return attachPoint;
    }

    public Matrix4 getTransform() {
        return transform;
    }

    public Matrix4 getRotation() {
        return rotation;
    }

    public float getScale() {
        return scale;
    }

    public boolean isMirrored() {
        return mirrored;
    }

    public String getRefusal() {
        return refusal;
    }

    public boolean isShown() {
        return shown;
    }

    public boolean isValid() {
        return shown && refusal == null;
    }
}
