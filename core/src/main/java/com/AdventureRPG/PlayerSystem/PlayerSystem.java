package com.AdventureRPG.PlayerSystem;

import com.AdventureRPG.GameManager;
import com.AdventureRPG.SettingsSystem.Settings;
import com.badlogic.gdx.graphics.PerspectiveCamera;

public class PlayerSystem {

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

    public void Awake() {
        camera.Awake();
    }

    public void Start() {

    }

    public void Update() {

    }

    public void Render() {

    }

    // Camera \\

    public PerspectiveCamera GetCamera() {
        return camera.Get();
    }
}
