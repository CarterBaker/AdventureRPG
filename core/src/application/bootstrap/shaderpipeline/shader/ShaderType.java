package application.bootstrap.shaderpipeline.shader;

public enum ShaderType {

    /*
     * Classifies a shader record by its role. VERT, FRAG, and INCLUDE are source
     * files parsed from disk. PROGRAM is a compiled GPU program assembled from a
     * JSON descriptor.
     */

    VERT,
    FRAG,
    INCLUDE,
    PROGRAM,
    TCS,
    TES
}