package com.internal.bootstrap.shaderpipeline.Shader;

/*
 * Classifies a GLSL source file by its role in the pipeline.
 * INCLUDE files are flattened into vert and frag sources during preprocessing
 * and never compiled as standalone programs.
 */
public enum ShaderType {
    VERT,
    FRAG,
    INCLUDE
}