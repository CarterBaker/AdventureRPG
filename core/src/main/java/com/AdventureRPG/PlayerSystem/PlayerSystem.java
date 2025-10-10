package com.AdventureRPG.PlayerSystem;

import com.AdventureRPG.GameManager;
import com.AdventureRPG.SettingsSystem.Settings;
import com.AdventureRPG.Util.Vector2Int;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;

public class PlayerSystem {

    // Debbug
    private final boolean debug = false; // TODO: Debug line

    // Game Manager
    private final Settings settings;

    // Player
    public final Statistics stats;
    public final PlayerCamera camera;
    public final PlayerPosition position;

    // Base \\

    public PlayerSystem(GameManager gameManager) {

        // Game Manager
        this.settings = gameManager.settings;

        // Player
        this.stats = new Statistics();
        this.camera = new PlayerCamera(settings.FOV, settings.windowWidth, settings.windowHeight);
        this.position = new PlayerPosition(gameManager, this);
    }

    public void awake() {
        camera.awake();
        position.awake();
    }

    public void start() {

    }

    public void update() {

        if (debug) // TODO: Debug line
            debug();
    }

    public void render() {

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

    // Debug \\

    private void debug() { // TODO: Debug line
        System.out.println(camera.get().position.set(position.currentPosition()).toString());
    }
}
