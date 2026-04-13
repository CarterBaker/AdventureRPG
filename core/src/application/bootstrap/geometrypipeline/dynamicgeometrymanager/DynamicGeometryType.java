package application.bootstrap.geometrypipeline.dynamicgeometrymanager;

public enum DynamicGeometryType {

    /*
     * Classifies how a block's faces are assembled into geometry. Determines
     * which geometry branch InternalBuildManager routes to per block per face.
     */

    NONE,
    FULL,
    PARTIAL,
    COMPLEX,
    LIQUID
}