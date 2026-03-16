package com.internal.bootstrap.worldpipeline.megastreammanager;

enum MegaQueueOperation {

    /*
     * Branch dispatch targets returned by MegaQueueManager.determineOperation.
     * Maps each MegaData stage to the branch responsible for executing it.
     */

    ASSESS,
    RENDER,
    DUMP,
    SKIP
}