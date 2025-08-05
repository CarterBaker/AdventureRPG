package com.AdventureRPG.PlayerSystem;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;

public class PlayerCamera {

    private final PerspectiveCamera perspectiveCamera;
    private float yaw = 0f;
    private float pitch = 0f;

    public PlayerCamera(float fov, float viewportWidth, float viewportHeight) {
        perspectiveCamera = new PerspectiveCamera(fov, viewportWidth, viewportHeight);
    }

    public void Awake() {
        perspectiveCamera.near = 0.1f;
        perspectiveCamera.far = 1000f;

        // Initial position & direction setup
        perspectiveCamera.position.set(0, 0, 0);
        perspectiveCamera.lookAt(0f, 0f, 0f);
        perspectiveCamera.up.set(Vector3.Y);
        perspectiveCamera.update();
    }

    public void updateViewport(float width, float height) {
        perspectiveCamera.viewportWidth = width;
        perspectiveCamera.viewportHeight = height;
        perspectiveCamera.update();
    }

    public void rotate(float deltaYaw, float deltaPitch) {
        yaw += deltaYaw;
        pitch += deltaPitch;

        // Clamp pitch to avoid flipping
        pitch = Math.max(-89f, Math.min(89f, pitch));

        // Calculate direction vector from yaw/pitch
        Vector3 direction = new Vector3(
                (float) Math.cos(Math.toRadians(yaw)) * (float) Math.cos(Math.toRadians(pitch)),
                (float) Math.sin(Math.toRadians(pitch)),
                (float) Math.sin(Math.toRadians(yaw)) * (float) Math.cos(Math.toRadians(pitch)));

        direction.nor(); // normalize

        perspectiveCamera.direction.set(direction);
        perspectiveCamera.update();
    }

    public PerspectiveCamera getCamera() {
        return perspectiveCamera;
    }

    public Vector3 Direction() {
        return perspectiveCamera.direction;
    }
}
