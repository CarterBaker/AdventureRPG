package application.bootstrap.weatherpipeline.weather;

public enum CloudType {

    /*
     * Cloud silhouette style read by the weather UBO push and, eventually,
     * the sky shader's cloud pass. CLEAR carries no cloud geometry at all.
     */

    CLEAR,
    STRATUS,
    CUMULUS,
    NIMBUS
}