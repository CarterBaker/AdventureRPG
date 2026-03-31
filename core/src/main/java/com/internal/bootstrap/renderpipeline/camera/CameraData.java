package com.internal.bootstrap.renderpipeline.camera;

import com.internal.platform.graphics.PerspectiveCamera;
import com.internal.core.engine.DataPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.mathematics.matrices.Matrix4;
import com.internal.core.util.mathematics.vectors.Vector2;
import com.internal.core.util.mathematics.vectors.Vector3;

public class CameraData extends DataPackage {

    /*
     * Raw perspective camera state. Owns the platform PerspectiveCamera and all
     * derived matrix and vector caches updated in place on every mutation.
     * Created with new — owned by CameraInstance.
     */

    // Internal
    private final PerspectiveCamera perspectiveCamera;

    // Cached Matrices
    private final Matrix4 projectionMat;
    private final Matrix4 viewMat;
    private final Matrix4 viewProjectionMat;
    private final Matrix4 inverseProjectionMat;
    private final Matrix4 inverseViewMat;

    // Cached Vectors
    private final Vector3 positionVec;
    private final Vector3 directionVec;
    private final Vector3 upVec;
    private final Vector2 viewportVec;

    // Constructor \\

    public CameraData(float fov, float viewportWidth, float viewportHeight) {

        // Cached Matrices
        this.projectionMat = new Matrix4();
        this.viewMat = new Matrix4();
        this.viewProjectionMat = new Matrix4();
        this.inverseProjectionMat = new Matrix4();
        this.inverseViewMat = new Matrix4();

        // Cached Vectors
        this.positionVec = new Vector3();
        this.directionVec = new Vector3();
        this.upVec = new Vector3();
        this.viewportVec = new Vector2();

        // Internal
        this.perspectiveCamera = new PerspectiveCamera(fov, viewportWidth, viewportHeight);
        this.perspectiveCamera.near = EngineSetting.CAMERA_NEAR_PLANE;
        this.perspectiveCamera.far = EngineSetting.CAMERA_FAR_PLANE;
        this.perspectiveCamera.position.set(0, 0, 0);
        this.perspectiveCamera.direction.set(0, 0, -1);
        this.perspectiveCamera.up.set(0, 1, 0);
        this.perspectiveCamera.update();

        syncCaches();
    }

    // Mutation \\

    public void setRotation(Vector2 input) {

        float yaw = (float) Math.atan2(perspectiveCamera.direction.x, perspectiveCamera.direction.z);
        float pitch = (float) Math.asin(-perspectiveCamera.direction.y);

        yaw -= Math.toRadians(input.x);
        pitch += Math.toRadians(input.y);

        float maxPitch = (float) Math.toRadians(EngineSetting.CAMERA_MAX_PITCH_DEGREES);
        pitch = Math.max(-maxPitch, Math.min(maxPitch, pitch));

        float cosPitch = (float) Math.cos(pitch);
        perspectiveCamera.direction.x = (float) Math.sin(yaw) * cosPitch;
        perspectiveCamera.direction.y = -(float) Math.sin(pitch);
        perspectiveCamera.direction.z = (float) Math.cos(yaw) * cosPitch;
        perspectiveCamera.up.set(0, 1, 0);
        perspectiveCamera.update();

        syncCaches();
    }

    public void setPosition(Vector3 input) {
        perspectiveCamera.position.set(input.x, input.y, input.z);
        perspectiveCamera.update();
        syncCaches();
    }

    public void updateViewport(float width, float height) {
        perspectiveCamera.viewportWidth = width;
        perspectiveCamera.viewportHeight = height;
        perspectiveCamera.update();
        syncCaches();
    }

    private void syncCaches() {

        projectionMat.fromGDX(perspectiveCamera.projection);
        viewMat.fromGDX(perspectiveCamera.view);
        viewProjectionMat.fromGDX(perspectiveCamera.combined);

        inverseProjectionMat.set(projectionMat).inverse();
        inverseViewMat.set(viewMat).inverse();

        positionVec.set(
                perspectiveCamera.position.x,
                perspectiveCamera.position.y,
                perspectiveCamera.position.z);
        directionVec.set(
                perspectiveCamera.direction.x,
                perspectiveCamera.direction.y,
                perspectiveCamera.direction.z);
        upVec.set(
                perspectiveCamera.up.x,
                perspectiveCamera.up.y,
                perspectiveCamera.up.z);
        viewportVec.set(
                perspectiveCamera.viewportWidth,
                perspectiveCamera.viewportHeight);
    }

    // Accessible \\

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
        return perspectiveCamera.fieldOfView;
    }

    public float getNearPlane() {
        return perspectiveCamera.near;
    }

    public float getFarPlane() {
        return perspectiveCamera.far;
    }

    public PerspectiveCamera getPerspectiveCamera() {
        return perspectiveCamera;
    }
}