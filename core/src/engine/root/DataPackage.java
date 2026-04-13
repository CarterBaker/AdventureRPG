package engine.root;

public abstract class DataPackage extends StructPackage {
    /*
     * DataPackages are intended to serve as the raw data payload held
     * within a HandlePackage or InstancePackage.
     *
     * They are simple, durable data containers with no lifecycle or
     * engine timing rules, and may persist for as long as the owning
     * Handle or Instance requires them.
     */
}
