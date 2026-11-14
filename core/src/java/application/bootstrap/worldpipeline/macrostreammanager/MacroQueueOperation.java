package application.bootstrap.worldpipeline.macrostreammanager;

enum MacroQueueOperation {

    /*
     * Branch dispatch targets returned by MacroQueueManager.determineOperation.
     * BUILD samples and assembles the macro's mesh on the MacroStreaming pool,
     * RENDER uploads it on the main thread.
     */

    BUILD,
    RENDER,
    SKIP
}
