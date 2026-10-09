package application.bootstrap.worldpipeline.megastreammanager;

enum MegaQueueOperation {

    /*
     * Branch dispatch targets returned by MegaQueueManager.determineOperation.
     * Maps each MegaData stage to the branch responsible for executing it.
     */

    ASSESS,
    RENDER,
    TREE_BUILD,
    TREE_RENDER,
    DUMP,
    SKIP
}