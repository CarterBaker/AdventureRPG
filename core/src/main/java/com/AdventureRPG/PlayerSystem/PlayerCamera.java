package com.AdventureRPG.PlayerSystem;

import com.AdventureRPG.Core.Framework.GameSystem;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;

public class PlayerCamera extends GameSystem {

    // Camera
    private PerspectiveCamera perspectiveCamera;
    private Vector3 direction;
    private float yaw = 0f;
    private float pitch = 0f;

    // Base \\

    public PlayerCamera(
            float fov,
            float viewportWidth,
            float viewportHeight) {

        // Camera
        perspectiveCamera = new PerspectiveCamera(
                fov,
                viewportWidth,
                viewportHeight);
    }

    @Override
    public void init() {

        // Camera
        direction = new Vector3();
    }

    @Override
    public void awake() {

        // Initial camera settings
        perspectiveCamera.near = 0.1f;
        perspectiveCamera.far = 1000f;

        // Initial position & direction setup
        perspectiveCamera.position.set(0, 0, 0);
        perspectiveCamera.lookAt(0f, 0f, 0f);
        perspectiveCamera.up.set(Vector3.Y);
        perspectiveCamera.update();
    }

    // Camera \\

    public PerspectiveCamera get() {
        return perspectiveCamera;
    }

    public Vector3 direction() {
        return perspectiveCamera.direction;
    }

    // Called when changing window size
    public void updateViewport(float width, float height) {
        perspectiveCamera.viewportWidth = width;
        perspectiveCamera.viewportHeight = height;
        perspectiveCamera.update();
    }

    // Called from inputSystem to rotate the camera
    public void rotate(float deltaYaw, float deltaPitch) {
        yaw += deltaYaw;
        pitch += deltaPitch;

        // Clamp pitch to avoid flipping
        pitch = Math.max(-89f, Math.min(89f, pitch));

        // Calculate direction vector from yaw/pitch
        direction.x = (float) Math.cos(Math.toRadians(yaw)) * (float) Math.cos(Math.toRadians(pitch));
        direction.y = (float) Math.sin(Math.toRadians(pitch));
        direction.z = (float) Math.sin(Math.toRadians(yaw)) * (float) Math.cos(Math.toRadians(pitch));
        direction.nor(); // normalize

        perspectiveCamera.direction.set(direction);
        perspectiveCamera.update();
    }
}
