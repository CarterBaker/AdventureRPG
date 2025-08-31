package com.AdventureRPG.PlayerSystem;

import com.AdventureRPG.GameManager;
import com.AdventureRPG.SettingsSystem.Settings;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;

public class PlayerSystem {

    // Debbug
    private final boolean debug = false; // TODO: Debug line

    // Game Manager
    private final Settings settings;

    // Player
    public final Statistics stats;
    public final PlayerCamera camera;
    public final PlayerPosition position;

    // Model
    private final ModelInstance modelInstance;

    // Base \\

    public PlayerSystem(GameManager gameManager) {

        // Game Manager
        this.settings = gameManager.settings;

        // Player
        this.stats = new Statistics();
        this.camera = new PlayerCamera(settings.FOV, settings.windowWidth, settings.windowHeight);
        this.position = new PlayerPosition(gameManager, this);

        // Model
        this.modelInstance = new ModelInstance(new Model());
    }

    public void awake() {
        camera.awake();
    }

    public void start() {

    }

    public void update() {
        positionPlayer();
        positionCamera();

        if (debug) // TODO: Debug line
            debug();
    }

    public void render() {

    }

    // Camera \\

    public PerspectiveCamera getCamera() {
        return camera.get();
    }

    // Position \\

    public void positionPlayer() {
        modelInstance.transform.setToTranslation(position.currentPosition());
    }

    private void positionCamera() {
        camera.get().position.set(position.currentPosition());
        camera.get().update();
    }

    // Debug \\

    private void debug() { // TODO: Debug line
        System.out.println(camera.get().position.set(position.currentPosition()).toString());
    }
}
