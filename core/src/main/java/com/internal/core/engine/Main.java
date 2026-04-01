package com.internal.core.engine;

import com.internal.platform.PlatformRuntime;
import java.io.File;
import com.internal.platform.Game;
import com.google.gson.Gson;
import com.internal.core.engine.settings.Settings;

public class Main extends Game {

    /*
     * Main serves as the application entry point.
     * It bridges legacy backend with the internal engine, allowing
     * the engine to manage lifecycle, updates, and rendering
     * independently of the platform layer.
     *
     * Key responsibilities:
     * - Initialize and execute the internal engine
     * - Propagate frame delta time to the engine
     * - Coordinate shutdown and resource disposal
     */

    // Root
    private final File GAME_DIRECTORY;
    private final Settings settings;
    private final Gson gson;
    private final WindowPlatform windowPlatform;

    // Internal
    private GameEngine internal;

    // Base \\

    public Main(
            File GAME_DIRECTORY,
            Settings settings,
            Gson gson,
            WindowPlatform windowPlatform) {

        // Root
        this.GAME_DIRECTORY = GAME_DIRECTORY;
        this.settings = settings;
        this.gson = gson;
        this.windowPlatform = windowPlatform;
    }

    @Override
    public void create() {

        // Internal
        EnginePackage.setupConstructor(
                settings,
                this,
                GAME_DIRECTORY,
                gson,
                windowPlatform);
        this.internal = new GameEngine();
        EnginePackage.ENGINE_STRUCT.remove();
        internal.execute();
    }

    @Override
    public void render() {
        setDeltaTime();
        super.render();
        internal.execute();
    }

    @Override
    public void dispose() {

        // Internal Engine
        internal.setInternalState(EngineState.EXIT);

        // Settings
        handleGameWindow();
        handleSettingsFile();

        // Game
        super.dispose();
    }

    // Utility \\

    private void setDeltaTime() {
        internal.setDeltaTime(PlatformRuntime.graphics.getDeltaTime());
    }

    private void handleGameWindow() {
        if (getScreen() != null)
            getScreen().dispose();
    }

    private void handleSettingsFile() {

        if (!settings.debug)
            return;

        File settingsFile = new File(GAME_DIRECTORY, "settings.json");

        if (settingsFile.exists()) {
            boolean deleted = settingsFile.delete();
            if (!deleted)
                UtilityPackage.throwException("File: " + settingsFile.getName() + ", Failed to delete on editor close");
        }
    }
}
