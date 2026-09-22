package application.bootstrap.worldpipeline.structure;

public enum StructureElevationType {

    /*
     * How a placement resolves its vertical position. SURFACE sits the
     * structure's anchor floor on the terrain (or the water surface) at its
     * position. UNDERGROUND buries it a rolled depth below that. ABSOLUTE
     * uses a fixed world Y and is only meaningful for hand-authored
     * locations that declare one.
     */

    SURFACE,
    UNDERGROUND,
    ABSOLUTE
}
