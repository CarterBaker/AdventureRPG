package engine.assets.camera;

import engine.root.DataPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector3;

public class CameraData extends DataPackage {

    private final Matrix4 projectionMat;
    private final Matrix4 viewMat;
    private final Matrix4 viewProjectionMat;
    private final Matrix4 inverseProjectionMat;
    private final Matrix4 inverseViewMat;

    private final Vector3 positionVec;
    private final Vector3 directionVec;
    private final Vector3 upVec;
    private final Vector2 viewportVec;

    private final float fov;
    private final float nearPlane;
    private final float farPlane;

    public CameraData(float fov, float viewportWidth, float viewportHeight) {
        this.projectionMat = new Matrix4();
        this.viewMat = new Matrix4();
        this.viewProjectionMat = new Matrix4();
        this.inverseProjectionMat = new Matrix4();
        this.inverseViewMat = new Matrix4();

        this.positionVec = new Vector3(0f, 0f, 0f);
        this.directionVec = new Vector3(0f, 0f, -1f);
        this.upVec = new Vector3(0f, 1f, 0f);
        this.viewportVec = new Vector2(viewportWidth, viewportHeight);

        this.fov = fov;
        this.nearPlane = EngineSetting.CAMERA_NEAR_PLANE;
        this.farPlane = EngineSetting.CAMERA_FAR_PLANE;

        syncCaches();
    }

    public void setRotation(Vector2 input) {
        float yaw = (float) Math.atan2(directionVec.x, directionVec.z);
        float pitch = (float) Math.asin(-directionVec.y);

        yaw -= Math.toRadians(input.x);
        pitch += Math.toRadians(input.y);

        float maxPitch = (float) Math.toRadians(EngineSetting.CAMERA_MAX_PITCH_DEGREES);
        pitch = Math.max(-maxPitch, Math.min(maxPitch, pitch));

        float cosPitch = (float) Math.cos(pitch);
        directionVec.x = (float) Math.sin(yaw) * cosPitch;
        directionVec.y = -(float) Math.sin(pitch);
        directionVec.z = (float) Math.cos(yaw) * cosPitch;
        directionVec.normalize();
        upVec.set(0f, 1f, 0f);

        syncCaches();
    }

    public void setPosition(Vector3 input) {
        positionVec.set(input);
        syncCaches();
    }

    public void updateViewport(float width, float height) {
        viewportVec.set(width, height);
        syncCaches();
    }

    private void syncCaches() {
        setPerspective(projectionMat, fov, viewportVec.x, viewportVec.y, nearPlane, farPlane);
        setLookAt(viewMat, positionVec, directionVec, upVec);
        viewProjectionMat.set(projectionMat).multiply(viewMat);

        inverseProjectionMat.set(projectionMat).inverse();
        inverseViewMat.set(viewMat).inverse();
    }

    private void setPerspective(Matrix4 out, float fovDeg, float width, float height, float near, float far) {
        float safeWidth = Math.max(1f, width);
        float safeHeight = Math.max(1f, height);
        float safeFov = Math.max(1f, Math.min(179f, fovDeg));
        float aspect = safeWidth / safeHeight;
        float f = (float) (1.0 / Math.tan(Math.toRadians(safeFov) * 0.5));
        out.set(
                f / aspect, 0, 0, 0,
                0, f, 0, 0,
                0, 0, (far + near) / (near - far), (2f * far * near) / (near - far),
                0, 0, -1f, 0);
    }

    private void setLookAt(Matrix4 out, Vector3 pos, Vector3 dir, Vector3 up) {
        Vector3 f = new Vector3(dir).normalize();
        Vector3 s = new Vector3(f.y * up.z - f.z * up.y, f.z * up.x - f.x * up.z, f.x * up.y - f.y * up.x).normalize();
        Vector3 u = new Vector3(s.y * f.z - s.z * f.y, s.z * f.x - s.x * f.z, s.x * f.y - s.y * f.x);

        out.set(
                s.x, s.y, s.z, -(s.x * pos.x + s.y * pos.y + s.z * pos.z),
                u.x, u.y, u.z, -(u.x * pos.x + u.y * pos.y + u.z * pos.z),
                -f.x, -f.y, -f.z, (f.x * pos.x + f.y * pos.y + f.z * pos.z),
                0, 0, 0, 1);
    }

    public Matrix4 getProjection() {
        return projectionMat;
    }

    public Matrix4 getView() {
        return viewMat;
    }

    public Matrix4 getViewProjection() {
        return viewProjectionMat;
    }

    public Matrix4 getInverseProjection() {
        return inverseProjectionMat;
    }

    public Matrix4 getInverseView() {
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