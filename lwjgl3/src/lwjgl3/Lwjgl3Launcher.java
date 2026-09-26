package lwjgl3;

import engine.root.EngineSetting;
import engine.root.GameEngine;

public class Lwjgl3Launcher {

    /*
     * Entry point for the game client. The game has no console: output goes
     * to the session log, which is written to the log directory only if the
     * session errors or crashes.
     */

    // Entry \\

    public static void main(String[] args) {

        Lwjgl3LauncherUtility.configureAwtForEngineRasterization();

        if (StartupHelper.startNewJvmIfRequired())
            return;

        Lwjgl3LauncherUtility.launch(
                EngineSetting.LOG_SESSION_GAME,
                EngineSetting.SETTINGS_FILE_NAME,
                EngineSetting.LAUNCHER_WINDOW_TITLE,
                GameEngine::new);
    }
}
