package application.bootstrap.worldpipeline.worldrendermanager;

public enum RenderOperation {

    /*
     * Render state for a world slot. Tracks whether a slot is awaiting or
     * holding an individual or batched render contribution.
     */

    NONE,
    NEEDS_BATCH_RENDER,
    HAS_BATCH_RENDER,
    NEEDS_INDIVIDUAL_RENDER,
    HAS_INDIVIDUAL_RENDER
}