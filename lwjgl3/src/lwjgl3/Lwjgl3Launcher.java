package lwjgl3;

import engine.root.EngineSetting;
import engine.root.GameEngine;
import engine.root.LauncherEngine;

public class Lwjgl3Launcher {

    /*
     * Entry point for the game client. The launcher opens first, in its own
     * borderless, transparent window that streams nothing; Launch closes it
     * completely and the game starts in the same, already warm JVM, while
     * Quit ends there. The game has no console: output goes to the session
     * log, which is written to the log directory only if the session errors
     * or crashes.
     */

    // Entry \\

    public static void main(String[] args) {

        Lwjgl3LauncherUtility.configureAwtForEngineRasterization();

        if (StartupHelper.startNewJvmIfRequired())
            return;

        LauncherEngine launcherEngine = Lwjgl3LauncherUtility.launchBorderless(
                EngineSetting.LOG_SESSION_LAUNCHER,
                EngineSetting.SETTINGS_FILE_NAME,
                EngineSetting.GAME_LAUNCHER_WINDOW_TITLE,
                EngineSetting.GAME_LAUNCHER_WINDOW_WIDTH,
                EngineSetting.GAME_LAUNCHER_WINDOW_HEIGHT,
                LauncherEngine::new);

        if (!launcherEngine.isLaunchRequested())
            return;

        Lwjgl3LauncherUtility.launch(
                EngineSetting.LOG_SESSION_GAME,
                EngineSetting.SETTINGS_FILE_NAME,
                EngineSetting.LAUNCHER_WINDOW_TITLE,
                GameEngine::new);
    }
}
