package program.bootstrap.shaderpipeline.shader;

/*
 * Classifies a shader record by its role. VERT, FRAG, and INCLUDE are source
 * files parsed from disk. PROGRAM is a compiled GPU program assembled from a
 * JSON descriptor.
 */
public enum ShaderType {
    VERT,
    FRAG,
    INCLUDE,
    PROGRAM
}