package application.bootstrap.worldpipeline.structure;

public enum StructureSurfaceType {

    /*
     * Which kind of ground a structure's anchor column must stand on, judged
     * by the same per-column flooding decision terrain generation makes.
     */

    LAND, // Anchor column must be dry
    UNDERWATER, // Anchor column must be flooded
    ANY // Anchor column may be either
}
