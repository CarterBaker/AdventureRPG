package com.AdventureRPG.core.renderer.camerasystem;

import com.AdventureRPG.core.engine.InstanceFrame;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class CameraInstance extends InstanceFrame {
    // Camera
    private PerspectiveCamera perspectiveCamera;
    private Vector3 direction;
    private float yaw = 0f;
    private float pitch = 0f;

    // Cached inverse matrices
    private Matrix4 inverseProjection = new Matrix4();
    private Matrix4 inverseView = new Matrix4();
    private Matrix4 viewProjection = new Matrix4();

    // Base \\
    public CameraInstance(
            float fov,
            float viewportWidth,
            float viewportHeight) {
        // Camera
        perspectiveCamera = new PerspectiveCamera(
                fov,
                viewportWidth,
                viewportHeight);
        direction = new Vector3();
        // Initial camera settings
        perspectiveCamera.near = 0.1f;
        perspectiveCamera.far = 1000f;
        // Initial position & direction setup
        perspectiveCamera.position.set(0, 0, 0);
        perspectiveCamera.lookAt(0f, 0f, 1f);
        perspectiveCamera.up.set(Vector3.Y);
        perspectiveCamera.update();
        updateCachedMatrices();
    }

    // Camera \\
    public Vector3 direction() {
        return perspectiveCamera.direction;
    }

    public void updateViewport(
            float width,
            float height) {
        perspectiveCamera.viewportWidth = width;
        perspectiveCamera.viewportHeight = height;
        perspectiveCamera.update();
        updateCachedMatrices();
    }

    public void setRotation(Vector2 input) {
        yaw += input.x;
        pitch += input.y;
        // Clamp pitch to avoid flipping
        pitch = Math.max(-89f, Math.min(89f, pitch));
        // Calculate direction vector from yaw/pitch
        direction.x = (float) Math.cos(Math.toRadians(yaw)) * (float) Math.cos(Math.toRadians(pitch));
        direction.y = (float) Math.sin(Math.toRadians(pitch));
        direction.z = (float) Math.sin(Math.toRadians(yaw)) * (float) Math.cos(Math.toRadians(pitch));
        direction.nor(); // normalize
        perspectiveCamera.direction.set(direction);
        perspectiveCamera.update();
        updateCachedMatrices();
    }

    public void setPosition(Vector3 input) {
        perspectiveCamera.position.set(input.x, input.y, input.z);
        perspectiveCamera.update();
        updateCachedMatrices();
    }

    private void updateCachedMatrices() {
        inverseProjection.set(perspectiveCamera.projection).inv();
        inverseView.set(perspectiveCamera.view).inv();
        viewProjection.set(perspectiveCamera.combined);
    }

    // Accessible \\
    public PerspectiveCamera getPerspectiveCamera() {
        return perspectiveCamera;
    }

    // UBO Getters \\
    public Matrix4 getProjection() {
        return perspectiveCamera.projection;
    }

    public Matrix4 getView() {
        return perspectiveCamera.view;
    }

    public Matrix4 getInverseProjection() {
        return inverseProjection;
    }

    public Matrix4 getInverseView() {
        return inverseView;
    }

    public Matrix4 getViewProjection() {
        return viewProjection;
    }

    public Vector3 getPosition() {
        return perspectiveCamera.position;
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
}