package com.AdventureRPG.Core.RenderPipeline.CameraSystem;

import com.AdventureRPG.Core.Bootstrap.InstanceFrame;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class CameraInstance extends InstanceFrame {

    // Camera
    private PerspectiveCamera perspectiveCamera;
    private Vector3 direction;
    private float yaw = 0f;
    private float pitch = 0f;

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
    }

    public void setPosition(Vector3 input) {

        perspectiveCamera.position.set(input.x, input.y, input.z);
        perspectiveCamera.update();
    }

    // Accessible \\

    public PerspectiveCamera getPerspectiveCamera() {
        return perspectiveCamera;
    }
}
