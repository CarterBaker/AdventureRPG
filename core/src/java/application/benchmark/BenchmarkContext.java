package application.benchmark;

import application.benchmark.stream.StreamBenchmarkSystem;
import engine.root.ContextPackage;

public class BenchmarkContext extends ContextPackage {

    /*
     * Context paired with the benchmark engine's hidden window. Owns the one
     * system that measures the world streaming routines over a fixed corpus.
     * Nothing else runs — no menu, grid, player or sky — so nothing streams
     * beside the measured calls.
     */

    // Runtime
    private StreamBenchmarkSystem streamBenchmarkSystem;

    // Internal \\

    @Override
    protected void create() {

        // Runtime
        this.streamBenchmarkSystem = create(StreamBenchmarkSystem.class);
    }

    @Override
    protected void awake() {
        getWindow().setCaptureEligible(false);
    }

    // Accessible \\

    public String getReport() {
        return streamBenchmarkSystem.getReport();
    }

    public long getGenerationChecksum() {
        return streamBenchmarkSystem.getGenerationChecksum();
    }

    public long getMeshingChecksum() {
        return streamBenchmarkSystem.getMeshingChecksum();
    }
}
