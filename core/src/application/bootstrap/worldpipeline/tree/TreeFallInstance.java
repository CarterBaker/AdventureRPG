package application.bootstrap.worldpipeline.tree;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.geometrypipeline.mesh.MeshInstance;
import engine.root.InstancePackage;
import engine.util.mathematics.matrices.Matrix4;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TreeFallInstance extends InstancePackage {

    /*
     * One piece a cut freed from a tree, falling free of it: the tree it came
     * from, the shape it carries away, the cut it hinges on, in blocks from
     * the centre of the tree's root, and the way it falls. A piece standing upright topples about its
     * cut, away from whoever cut it, gathering speed as a felled trunk does;
     * any other drops straight down. Its geometry is written by a worker and
     * published whole, then uploaded on the main thread as meshes in the
     * chunk vertex layout. The one who felled it is who its drops fly from.
     */

    // Source
    private TreeInstance tree;
    private TreeShapeStruct piece;
    private EntityInstance feller;

    // Hinge — blocks from the centre of the tree's root
    private float pivotX;
    private float pivotY;
    private float pivotZ;
    private float headingX;
    private float headingZ;
    private boolean toppling;

    // Motion
    private TreeFallState state;
    private float angle;
    private float angularSpeed;
    private float drop;
    private float dropSpeed;
    private float restSeconds;

    // Geometry — written by the worker, then uploaded
    private volatile FloatArrayList builtBark;
    private volatile FloatArrayList builtLeaves;
    private ObjectArrayList<MeshInstance> barkMeshes;
    private ObjectArrayList<MeshInstance> leafMeshes;

    // Constructor \\

    public void constructor(
            TreeInstance tree,
            TreeShapeStruct piece,
            EntityInstance feller,
            float pivotX,
            float pivotY,
            float pivotZ,
            float headingX,
            float headingZ,
            boolean toppling) {

        // Source
        this.tree = tree;
        this.piece = piece;
        this.feller = feller;

        // Hinge
        this.pivotX = pivotX;
        this.pivotY = pivotY;
        this.pivotZ = pivotZ;
        this.headingX = headingX;
        this.headingZ = headingZ;
        this.toppling = toppling;

        // Motion
        this.state = TreeFallState.BUILDING;

        // Geometry
        this.barkMeshes = new ObjectArrayList<>();
        this.leafMeshes = new ObjectArrayList<>();
    }

    // Geometry \\

    // Worker — the piece's geometry, published whole
    public void publishGeometry(FloatArrayList bark, FloatArrayList leaves) {
        this.builtLeaves = leaves;
        this.builtBark = bark;
    }

    public boolean isBuilt() {
        return builtBark != null;
    }

    // Motion \\

    public void setState(TreeFallState state) {
        this.state = state;
    }

    public void setAngle(float angle, float angularSpeed) {
        this.angle = angle;
        this.angularSpeed = angularSpeed;
    }

    public void setDrop(float drop, float dropSpeed) {
        this.drop = drop;
        this.dropSpeed = dropSpeed;
    }

    public void setRestSeconds(float restSeconds) {
        this.restSeconds = restSeconds;
    }

    // Pose \\

    // Carries a point of the piece, in blocks from the centre of its tree's root, to where it lies now in a frame
    // that root centre stands at; the tilt turns the piece's up toward its heading about the hinge
    public void transformPoint(float x, float y, float z, float rootX, float rootY, float rootZ, float[] out) {

        float axisX = headingZ;
        float axisZ = -headingX;
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        float turn = 1f - cos;
        float localX = x - pivotX;
        float localY = y - pivotY;
        float localZ = z - pivotZ;

        out[0] = rootX + pivotX + (cos + turn * axisX * axisX) * localX - sin * axisZ * localY
                + turn * axisX * axisZ * localZ;
        out[1] = rootY + pivotY - drop + sin * axisZ * localX + cos * localY - sin * axisX * localZ;
        out[2] = rootZ + pivotZ + turn * axisZ * axisX * localX + sin * axisX * localY
                + (cos + turn * axisZ * axisZ) * localZ;
    }

    // The same carry as a matrix, for drawing
    public void composeMatrix(float rootX, float rootY, float rootZ, Matrix4 out) {

        float axisX = headingZ;
        float axisZ = -headingX;
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        float turn = 1f - cos;
        float r00 = cos + turn * axisX * axisX;
        float r01 = -sin * axisZ;
        float r02 = turn * axisX * axisZ;
        float r10 = sin * axisZ;
        float r11 = cos;
        float r12 = -sin * axisX;
        float r20 = turn * axisZ * axisX;
        float r21 = sin * axisX;
        float r22 = cos + turn * axisZ * axisZ;

        out.set(
                r00, r01, r02, rootX + pivotX - (r00 * pivotX + r01 * pivotY + r02 * pivotZ),
                r10, r11, r12, rootY + pivotY - drop - (r10 * pivotX + r11 * pivotY + r12 * pivotZ),
                r20, r21, r22, rootZ + pivotZ - (r20 * pivotX + r21 * pivotY + r22 * pivotZ),
                0f, 0f, 0f, 1f);
    }

    // Accessible \\

    public TreeInstance getTree() {
        return tree;
    }

    public TreeShapeStruct getPiece() {
        return piece;
    }

    public EntityInstance getFeller() {
        return feller;
    }

    public float getPivotX() {
        return pivotX;
    }

    public float getPivotY() {
        return pivotY;
    }

    public float getPivotZ() {
        return pivotZ;
    }

    public float getHeadingX() {
        return headingX;
    }

    public float getHeadingZ() {
        return headingZ;
    }

    public boolean isToppling() {
        return toppling;
    }

    public TreeFallState getState() {
        return state;
    }

    public float getAngle() {
        return angle;
    }

    public float getAngularSpeed() {
        return angularSpeed;
    }

    public float getDrop() {
        return drop;
    }

    public float getDropSpeed() {
        return dropSpeed;
    }

    public float getRestSeconds() {
        return restSeconds;
    }

    public FloatArrayList getBuiltBark() {
        return builtBark;
    }

    public FloatArrayList getBuiltLeaves() {
        return builtLeaves;
    }

    public ObjectArrayList<MeshInstance> getBarkMeshes() {
        return barkMeshes;
    }

    public ObjectArrayList<MeshInstance> getLeafMeshes() {
        return leafMeshes;
    }
}
