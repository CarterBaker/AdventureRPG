package engine.root;

public abstract class BuilderPackage extends SystemPackage {

    /*
     * Base class for builders. A builder is created and owned by its
     * LoaderPackage, receives files one at a time from the loader's load(), and
     * is released together with it once the queue is empty.
     */
}