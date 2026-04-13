package engine.root;

public abstract class BuilderPackage extends SystemPackage {

    /*
     * Base class for all builder systems managed by a LoaderPackage.
     * Builders are created and owned by their loader, and are automatically
     * released when the loader finishes processing its file queue.
     *
     * BuilderPackage deliberately provides very little structure — its
     * primary purpose is to be a lifecycle-aware unit of work that receives
     * files from its parent loader one at a time via `build(File)`.
     *
     * Key responsibilities:
     * - Receive individual files from a LoaderPackage during UPDATE
     * - Implement `build(File)` to define per-file processing logic
     * - Participate in the standard system lifecycle (create, get, awake, etc.)
     *
     * Lifecycle:
     * Builders follow the standard SystemPackage lifecycle.
     * `build(File)` is called externally by the owning LoaderPackage,
     * typically from within the loader's `load(File)` override.
     */
}