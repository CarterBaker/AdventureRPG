package com.AdventureRPG.core.renderpipeline.camerasystem;

import com.AdventureRPG.core.engine.InstancePackage;
import com.AdventureRPG.core.util.mathematics.matrices.Matrix4;
import com.AdventureRPG.core.util.mathematics.vectors.Vector2;
import com.AdventureRPG.core.util.mathematics.vectors.Vector3;
import com.badlogic.gdx.graphics.PerspectiveCamera;

public class CameraInstance extends InstancePackage {

    private PerspectiveCamera perspectiveCamera;

    // Internal (custom) math caches
    private Vector3 positionVec = new Vector3();
    private Vector3 directionVec = new Vector3();
    private Vector3 upVec = new Vector3();
    private Matrix4 projectionMat = new Matrix4();
    private Matrix4 viewMat = new Matrix4();
    private Matrix4 viewProjectionMat = new Matrix4();
    private Matrix4 inverseProjectionMat = new Matrix4();
    private Matrix4 inverseViewMat = new Matrix4();

    private float yaw = 0;
    private float pitch = 0;

    public void constructor(float fov, float viewportWidth, float viewportHeight) {

        perspectiveCamera = new PerspectiveCamera(fov, viewportWidth, viewportHeight);
        perspectiveCamera.near = 0.1f;
        perspectiveCamera.far = 1000f;
        perspectiveCamera.position.set(0, 0, 0);
        perspectiveCamera.direction.set(0, 0, -1);
        perspectiveCamera.up.set(0, 1, 0);
        perspectiveCamera.update();

        updateCachedValues();
    }

    /** Update cached matrices and vectors in-place */
    private void updateCachedValues() {
        // Update matrices from LibGDX camera
        projectionMat.fromGDX(perspectiveCamera.projection);
        viewMat.fromGDX(perspectiveCamera.view);
        viewProjectionMat.fromGDX(perspectiveCamera.combined);

        inverseProjectionMat.set(projectionMat).inverse();
        inverseViewMat.set(viewMat).inverse();

        // Update vectors
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
    }

    /** Update camera rotation (FPS-style, stable, no flips) */
    public void setRotation(Vector2 input) {
        // Update yaw/pitch from input
        yaw -= input.x;
        pitch -= input.y;

        // Clamp pitch to avoid singularity
        pitch = Math.max(-89.9f, Math.min(89.9f, pitch));

        // Convert to radians
        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);

        // Compute forward vector using yaw/pitch (spherical coordinates)
        float cosPitch = (float) Math.cos(pitchRad);
        directionVec.x = cosPitch * (float) Math.sin(yawRad);
        directionVec.y = (float) Math.sin(pitchRad);
        directionVec.z = -cosPitch * (float) Math.cos(yawRad);
        normalize(directionVec);

        // CRITICAL: Build right vector directly from yaw (independent of pitch)
        // This is ALWAYS perpendicular to world up, avoiding gimbal lock
        Vector3 right = new Vector3();
        right.x = (float) Math.cos(yawRad); // Perpendicular to yaw direction
        right.y = 0.0f; // Always horizontal
        right.z = (float) Math.sin(yawRad);
        normalize(right);

        // Compute up vector as cross of forward and right
        upVec.set(cross(directionVec, right));
        normalize(upVec);

        // Apply to LibGDX camera
        perspectiveCamera.direction.set(directionVec.x, directionVec.y, directionVec.z);
        perspectiveCamera.up.set(upVec.x, upVec.y, upVec.z);
        perspectiveCamera.update();

        updateCachedValues();
    }

    private Vector3 cross(Vector3 a, Vector3 b) {
        return new Vector3(
                a.y * b.z - a.z * b.y,
                a.z * b.x - a.x * b.z,
                a.x * b.y - a.y * b.x);
    }

    private void normalize(Vector3 v) {
        float len = (float) Math.sqrt(
                v.x * v.x +
                        v.y * v.y +
                        v.z * v.z);

        if (len == 0f)
            return;

        float inv = 1.0f / len;
        v.x *= inv;
        v.y *= inv;
        v.z *= inv;
    }

    /** Set camera position */
    public void setPosition(Vector3 input) {
        perspectiveCamera.position.set(input.x, input.y, input.z);
        perspectiveCamera.update();
        updateCachedValues();
    }

    /** Update viewport size */
    public void updateViewport(float width, float height) {
        perspectiveCamera.viewportWidth = width;
        perspectiveCamera.viewportHeight = height;
        perspectiveCamera.update();
        updateCachedValues();
    }

    // -------------------------
    // Getters return internal math objects directly
    // -------------------------

    public Vector3 getPosition() {
        return positionVec;
    }

    public Vector3 getDirection() {
        return directionVec;
    }

    public Vector3 getUp() {
        return upVec;
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

    public float getFOV() {
        return perspectiveCamera.fieldOfView;
    }

    public Vector2 getViewport() {
        return new Vector2(perspectiveCamera.viewportWidth, perspectiveCamera.viewportHeight);
    }

    public float getNearPlane() {
        return perspectiveCamera.near;
    }

    public float getFarPlane() {
        return perspectiveCamera.far;
    }

    // Optional: expose the underlying LibGDX camera
    public PerspectiveCamera getPerspectiveCamera() {
        return perspectiveCamera;
    }
}
