package application.bootstrap.worldpipeline.structure;

public enum StructureSurfaceType {

    /*
     * Which kind of ground a structure's anchor column must stand on, judged
     * by the same per-column flooding decision terrain generation makes, or
     * the floor of a dry cave below the ground, found down the anchor column
     * exactly as caves are carved.
     */

    LAND, // Anchor column must be dry
    UNDERWATER, // Anchor column must be flooded
    ANY, // Anchor column may be either
    CAVE // Anchor stands on a dry cave floor within the height range, with room above for the structure
}
