package lwjgl3;

import engine.root.EditorEngine;
import engine.root.EngineSetting;

public class Lwjgl3LauncherEditor {

    /*
     * Entry point for the editor. Output goes to the session log, readable in
     * the editor's Console tab and written to the log directory on any error.
     */

    // Entry \\

    public static void main(String[] args) {

        Lwjgl3LauncherUtility.configureAwtForEngineRasterization();

        if (StartupHelper.startNewJvmIfRequired())
            return;

        Lwjgl3LauncherUtility.launch(
                EngineSetting.LOG_SESSION_EDITOR,
                EngineSetting.EDITOR_SETTINGS_FILE_NAME,
                EngineSetting.LAUNCHER_EDITOR_WINDOW_TITLE,
                EditorEngine::new,
                EngineSetting.BIN_DIRECTORY + EngineSetting.PATH_SEPARATOR + EngineSetting.EDITOR_LAYOUT_DIRECTORY);
    }
}
