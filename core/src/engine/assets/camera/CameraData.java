package engine.assets.camera;

import engine.root.DataPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector3;

public class CameraData extends DataPackage {

    /*
     * Perspective camera state. Setters only mark the cached matrices stale;
     * projection, view and their inverses are rebuilt once, on the first
     * matrix read after a change, using scratch basis vectors so a frame of
     * camera movement never allocates.
     */

    // Matrices
    private final Matrix4 projectionMat;
    private final Matrix4 viewMat;
    private final Matrix4 viewProjectionMat;
    private final Matrix4 inverseProjectionMat;
    private final Matrix4 inverseViewMat;

    // Orientation
    private final Vector3 positionVec;
    private final Vector3 directionVec;
    private final Vector3 upVec;
    private final Vector2 viewportVec;

    // Scratch
    private final Vector3 forwardScratch;
    private final Vector3 sideScratch;
    private final Vector3 upScratch;

    // Lens
    private float fov;
    private final float nearPlane;
    private final float farPlane;

    // State
    private boolean dirty;

    // Constructor \\

    public CameraData(float fov, float viewportWidth, float viewportHeight) {

        // Matrices
        this.projectionMat = new Matrix4();
        this.viewMat = new Matrix4();
        this.viewProjectionMat = new Matrix4();
        this.inverseProjectionMat = new Matrix4();
        this.inverseViewMat = new Matrix4();

        // Orientation
        this.positionVec = new Vector3(0f, 0f, 0f);
        this.directionVec = new Vector3(0f, 0f, -1f);
        this.upVec = new Vector3(0f, 1f, 0f);
        this.viewportVec = new Vector2(viewportWidth, viewportHeight);

        // Scratch
        this.forwardScratch = new Vector3();
        this.sideScratch = new Vector3();
        this.upScratch = new Vector3();

        // Lens
        this.fov = fov;
        this.nearPlane = EngineSetting.CAMERA_NEAR_PLANE;
        this.farPlane = EngineSetting.CAMERA_FAR_PLANE;

        // State
        this.dirty = true;
    }

    // Management \\

    public void setRotation(Vector2 input) {

        double yaw = Math.atan2(directionVec.x, directionVec.z) - Math.toRadians(input.x);
        double pitch = Math.asin(-directionVec.y) + Math.toRadians(input.y);
        double maxPitch = Math.toRadians(EngineSetting.CAMERA_MAX_PITCH_DEGREES);

        pitch = Math.max(-maxPitch, Math.min(maxPitch, pitch));

        float cosPitch = (float) Math.cos(pitch);
        directionVec.x = (float) Math.sin(yaw) * cosPitch;
        directionVec.y = -(float) Math.sin(pitch);
        directionVec.z = (float) Math.cos(yaw) * cosPitch;
        directionVec.normalize();
        upVec.set(0f, 1f, 0f);

        this.dirty = true;
    }

    public void setPosition(Vector3 input) {
        positionVec.set(input);
        this.dirty = true;
    }

    public void setDirection(Vector3 input) {
        directionVec.set(input).normalize();
        upVec.set(0f, 1f, 0f);
        this.dirty = true;
    }

    public void updateViewport(float width, float height) {
        viewportVec.set(width, height);
        this.dirty = true;
    }

    public void setFOV(float fov) {
        this.fov = fov;
        this.dirty = true;
    }

    // Pick Ray \\

    public void getPickRay(float ndcX, float ndcY, Vector3 outOrigin, Vector3 outDirection) {

        float tanHalfFov = (float) Math.tan(Math.toRadians(getSafeFov()) * 0.5);
        float horizontal = ndcX * tanHalfFov * getSafeAspect();
        float vertical = ndcY * tanHalfFov;

        resolveBasis(directionVec, upVec);

        outOrigin.set(positionVec);
        outDirection.set(
                forwardScratch.x + sideScratch.x * horizontal + upScratch.x * vertical,
                forwardScratch.y + sideScratch.y * horizontal + upScratch.y * vertical,
                forwardScratch.z + sideScratch.z * horizontal + upScratch.z * vertical).normalize();
    }

    // Sync \\

    private void syncIfDirty() {

        if (!dirty)
            return;

        setPerspective(projectionMat);
        setLookAt(viewMat, positionVec);
        viewProjectionMat.set(projectionMat).multiply(viewMat);

        inverseProjectionMat.set(projectionMat).inverse();
        inverseViewMat.set(viewMat).inverse();

        this.dirty = false;
    }

    private void setPerspective(Matrix4 out) {

        float f = (float) (1.0 / Math.tan(Math.toRadians(getSafeFov()) * 0.5));
        float aspect = getSafeAspect();
        float near = nearPlane;
        float far = farPlane;

        out.set(
                f / aspect, 0, 0, 0,
                0, f, 0, 0,
                0, 0, (far + near) / (near - far), (2f * far * near) / (near - far),
                0, 0, -1f, 0);
    }

    private void setLookAt(Matrix4 out, Vector3 pos) {

        resolveBasis(directionVec, upVec);

        Vector3 f = forwardScratch;
        Vector3 s = sideScratch;
        Vector3 u = upScratch;

        out.set(
                s.x, s.y, s.z, -(s.x * pos.x + s.y * pos.y + s.z * pos.z),
                u.x, u.y, u.z, -(u.x * pos.x + u.y * pos.y + u.z * pos.z),
                -f.x, -f.y, -f.z, (f.x * pos.x + f.y * pos.y + f.z * pos.z),
                0, 0, 0, 1);
    }

    private void resolveBasis(Vector3 direction, Vector3 up) {

        Vector3 f = forwardScratch.set(direction).normalize();

        sideScratch.set(
                f.y * up.z - f.z * up.y,
                f.z * up.x - f.x * up.z,
                f.x * up.y - f.y * up.x).normalize();

        Vector3 s = sideScratch;

        upScratch.set(
                s.y * f.z - s.z * f.y,
                s.z * f.x - s.x * f.z,
                s.x * f.y - s.y * f.x);
    }

    private float getSafeFov() {
        return Math.max(EngineSetting.CAMERA_MIN_FOV_DEGREES, Math.min(EngineSetting.CAMERA_MAX_FOV_DEGREES, fov));
    }

    private float getSafeAspect() {

        float width = Math.max(EngineSetting.CAMERA_MIN_VIEWPORT_DIMENSION, viewportVec.x);
        float height = Math.max(EngineSetting.CAMERA_MIN_VIEWPORT_DIMENSION, viewportVec.y);

        return width / height;
    }

    // Accessible \\

    public Matrix4 getProjection() {
        syncIfDirty();
        return projectionMat;
    }

    public Matrix4 getView() {
        syncIfDirty();
        return viewMat;
    }

    public Matrix4 getViewProjection() {
        syncIfDirty();
        return viewProjectionMat;
    }

    public Matrix4 getInverseProjection() {
        syncIfDirty();
        return inverseProjectionMat;
    }

    public Matrix4 getInverseView() {
        syncIfDirty();
        return inverseViewMat;
    }

    public Vector3 getPosition() {
        return positionVec;
    }

    public Vector3 getDirection() {
        return directionVec;
    }

    public Vector3 getUp() {
        return upVec;
    }

    public Vector2 getViewport() {
        return viewportVec;
    }

    public float getFOV() {
        return fov;
    }

    public float getNearPlane() {
        return nearPlane;
    }

    public float getFarPlane() {
        return farPlane;
    }
}
