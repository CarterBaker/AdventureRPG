package application.bootstrap.geometrypipeline.dynamicgeometrymanager;

public enum DynamicGeometryType {

    /*
     * Classifies how a block's faces are assembled into geometry. Determines
     * which geometry branch GeometryBuildManager routes to per block per face.
     * PARTIAL is never declared by a block definition — it is the geometry of
     * a cell whose FULL block has been subdivided into sub-blocks.
     */

    NONE,
    FULL,
    PARTIAL,
    COMPLEX,
    LIQUID;

    public static final DynamicGeometryType[] VALUES = values();
    public static final int LENGTH = VALUES.length;
}