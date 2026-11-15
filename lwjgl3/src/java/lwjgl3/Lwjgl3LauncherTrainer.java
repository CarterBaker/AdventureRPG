package lwjgl3;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import engine.root.EngineSetting;
import engine.root.TrainerEngine;

public class Lwjgl3LauncherTrainer {

    /*
     * Entry point for the dialogue trainer, run through the lwjgl3:trainer
     * Gradle task. Boots the game's pipelines behind a hidden window and
     * holds practice talks over the console it was started from, which the
     * engine's session log takes over while it runs, so the console is handed
     * to the engine before it starts.
     */

    // Entry \\

    public static void main(String[] args) {

        Lwjgl3LauncherUtility.configureAwtForEngineRasterization();

        if (StartupHelper.startNewJvmIfRequired())
            return;

        PrintStream console = System.out;
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

        Lwjgl3LauncherUtility.launchHidden(
                EngineSetting.LOG_SESSION_TRAINER,
                EngineSetting.SETTINGS_FILE_NAME,
                EngineSetting.TRAINER_WINDOW_TITLE,
                EngineSetting.BENCHMARK_WINDOW_WIDTH,
                EngineSetting.BENCHMARK_WINDOW_HEIGHT,
                () -> new TrainerEngine(console, input));

        System.exit(EngineSetting.BENCHMARK_EXIT_SUCCESS);
    }
}
