package lwjgl3;

import java.io.PrintStream;

import engine.root.BenchmarkEngine;
import engine.root.EngineSetting;

public class Lwjgl3LauncherBenchmark {

    /*
     * Entry point for the benchmark tool, run through the lwjgl3:benchmark
     * Gradle task. Boots the game's pipelines behind a hidden window and times
     * the world streaming routines over a fixed corpus, first with the kernels
     * at the highest level the CPU runs, then once more for every lower
     * variant, each selected through the CPU level property, in the same JVM
     * the way the launcher hands over to the game. Prints every report to the
     * console it was started from, which the engine's session log takes over
     * while it runs, and exits non-zero when any variant's output differs from
     * the first run's.
     */

    // Entry \\

    public static void main(String[] args) {

        Lwjgl3LauncherUtility.configureAwtForEngineRasterization();

        if (StartupHelper.startNewJvmIfRequired())
            return;

        PrintStream console = System.out;
        BenchmarkEngine firstEngine = runBenchmark(console);
        boolean identical = true;

        for (int level = firstEngine.getKernelCpuLevel() - 1; level >= EngineSetting.KERNEL_CPU_LEVEL_V2; level--) {

            System.setProperty(EngineSetting.KERNEL_CPU_LEVEL_PROPERTY, Integer.toString(level));

            BenchmarkEngine engine = runBenchmark(console);

            identical &= engine.getGenerationChecksum() == firstEngine.getGenerationChecksum()
                    && engine.getMeshingChecksum() == firstEngine.getMeshingChecksum();
        }

        console.println(identical ? EngineSetting.BENCHMARK_IDENTICAL : EngineSetting.BENCHMARK_DIFFERENT);
        System.exit(identical ? EngineSetting.BENCHMARK_EXIT_SUCCESS : EngineSetting.BENCHMARK_EXIT_FAILURE);
    }

    // Run \\

    private static BenchmarkEngine runBenchmark(PrintStream console) {

        BenchmarkEngine benchmarkEngine = Lwjgl3LauncherUtility.launchHidden(
                EngineSetting.LOG_SESSION_BENCHMARK,
                EngineSetting.SETTINGS_FILE_NAME,
                EngineSetting.BENCHMARK_WINDOW_TITLE,
                EngineSetting.BENCHMARK_WINDOW_WIDTH,
                EngineSetting.BENCHMARK_WINDOW_HEIGHT,
                BenchmarkEngine::new);

        console.print(benchmarkEngine.getReport());

        return benchmarkEngine;
    }
}
