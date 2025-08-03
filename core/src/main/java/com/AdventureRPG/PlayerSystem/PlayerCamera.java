package com.AdventureRPG.PlayerSystem;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;

public class PlayerCamera {
    private final PerspectiveCamera perspectiveCamera;

    public PlayerCamera(float fov, float viewportWidth, float viewportHeight) {
        perspectiveCamera = new PerspectiveCamera(fov, viewportWidth, viewportHeight);
        perspectiveCamera.near = 0.1f;
        perspectiveCamera.far = 1000f;

        // Initial position & direction setup
        perspectiveCamera.position.set(0f, 10f, 10f);
        perspectiveCamera.lookAt(0f, 0f, 0f);
        perspectiveCamera.up.set(Vector3.Y);
        perspectiveCamera.update();
    }

    public void updateViewport(float width, float height) {
        perspectiveCamera.viewportWidth = width;
        perspectiveCamera.viewportHeight = height;
        perspectiveCamera.update();
    }

    public void update(Vector3 playerPosition) {
        Vector3 camPos = new Vector3(playerPosition.x, playerPosition.y + 10f, playerPosition.z + 10f);
        perspectiveCamera.position.set(camPos);
        perspectiveCamera.lookAt(playerPosition);
        perspectiveCamera.update();
    }

    public PerspectiveCamera getCamera() {
        return perspectiveCamera;
    }
}
