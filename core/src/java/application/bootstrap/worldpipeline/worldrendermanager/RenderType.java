package application.bootstrap.worldpipeline.worldrendermanager;

public enum RenderType {

    /*
     * Render path assigned to a WorldRenderInstance. INDIVIDUAL renders the
     * chunk directly. BATCHED merges it into a MegaChunkInstance. INVALID marks
     * a SubChunkInstance that never renders directly — only contributes to its
     * parent chunk's merged geometry.
     */

    INVALID,
    INDIVIDUAL,
    BATCHED
}