package com.AdventureRPG.PlayerSystem;

import com.AdventureRPG.Core.GameManager;
import com.AdventureRPG.Util.Vector2Int;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;

public class PlayerSystem extends GameManager {

    // Player
    public Statistics stats;
    public PlayerCamera camera;
    public PlayerPosition position;

    // Base \\

    @Override
    public void init() {

        // Player
        this.stats = (Statistics) register(new Statistics());
        this.camera = (PlayerCamera) register(new PlayerCamera(
                settings.FOV,
                settings.windowWidth,
                settings.windowHeight));
        this.position = (PlayerPosition) register(new PlayerPosition(this));
    }

    // Camera \\

    public PerspectiveCamera getCamera() {
        return camera.get();
    }

    // Accessible \\

    public Vector3 currentPosition() {
        return position.currentPosition();
    }

    public Vector2Int chunkCoordinate() {
        return position.chunkCoordinate();
    }
}
