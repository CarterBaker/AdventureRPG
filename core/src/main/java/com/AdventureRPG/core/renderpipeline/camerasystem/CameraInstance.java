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

    /** Set camera rotation */
    public void setRotation(Vector2 input) {

        // input.x = horizontal rotation (yaw)
        // input.y = vertical rotation (pitch)

        // Get current direction as angles
        float yaw = (float) Math.atan2(perspectiveCamera.direction.x, perspectiveCamera.direction.z);
        float pitch = (float) Math.asin(-perspectiveCamera.direction.y);

        // Apply rotation deltas
        yaw -= Math.toRadians(input.x);
        pitch += Math.toRadians(input.y);

        // Clamp pitch to prevent looking directly up/down (gimbal lock)
        // 89 degrees = ~1.553 radians
        float maxPitch = (float) Math.toRadians(89.0);
        pitch = Math.max(-maxPitch, Math.min(maxPitch, pitch));

        // Calculate new direction from angles
        float cosPitch = (float) Math.cos(pitch);
        perspectiveCamera.direction.x = (float) Math.sin(yaw) * cosPitch;
        perspectiveCamera.direction.y = -(float) Math.sin(pitch);
        perspectiveCamera.direction.z = (float) Math.cos(yaw) * cosPitch;

        // Keep up vector pointing up (prevents rolling)
        perspectiveCamera.up.set(0, 1, 0);

        // Debug tracking
        debugCameraOrientation();

        // Update camera
        perspectiveCamera.update();
        updateCachedValues();
    }

    //

    // Add these fields to your class
    private String lastDebugState = "";

    private void debugCameraOrientation() {
        // Get cardinal direction you're facing
        String facing = getCardinalDirection();

        // Get right vector (cross product of direction and up)
        com.badlogic.gdx.math.Vector3 gdxDir = perspectiveCamera.direction;
        com.badlogic.gdx.math.Vector3 gdxUp = perspectiveCamera.up;
        com.badlogic.gdx.math.Vector3 right = new com.badlogic.gdx.math.Vector3(gdxDir).crs(gdxUp);

        // Format state
        String currentState = String.format(
                "Facing: %s | Dir: (%.2f, %.2f, %.2f) | Up: (%.2f, %.2f, %.2f) | Right: (%.2f, %.2f, %.2f)",
                facing,
                gdxDir.x, gdxDir.y, gdxDir.z,
                gdxUp.x, gdxUp.y, gdxUp.z,
                right.x, right.y, right.z);

        // Only print if changed
        if (!currentState.equals(lastDebugState)) {
            System.out.println(currentState);
            lastDebugState = currentState;
        }
    }

    private String getCardinalDirection() {
        float x = perspectiveCamera.direction.x;
        float z = perspectiveCamera.direction.z;
        float angle = (float) Math.toDegrees(Math.atan2(x, z));
        if (angle < 0)
            angle += 360;

        if (angle < 45 || angle >= 315)
            return "NORTH";
        if (angle >= 45 && angle < 135)
            return "EAST";
        if (angle >= 135 && angle < 225)
            return "SOUTH";
        return "WEST";
    }
    //

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
