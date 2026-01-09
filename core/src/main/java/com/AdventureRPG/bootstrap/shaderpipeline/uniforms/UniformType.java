package com.AdventureRPG.bootstrap.shaderpipeline.uniforms;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public enum UniformType {

    // Scalars
    FLOAT("float"),
    DOUBLE("double"),
    INT("int"),
    BOOL("bool"),

    // Vectors
    VECTOR2("vec2"),
    VECTOR3("vec3"),
    VECTOR4("vec4"),
    VECTOR2_DOUBLE("dvec2"),
    VECTOR3_DOUBLE("dvec3"),
    VECTOR4_DOUBLE("dvec4"),
    VECTOR2_INT("ivec2"),
    VECTOR3_INT("ivec3"),
    VECTOR4_INT("ivec4"),
    VECTOR2_BOOLEAN("bvec2"),
    VECTOR3_BOOLEAN("bvec3"),
    VECTOR4_BOOLEAN("bvec4"),

    // Matrices
    MATRIX2("mat2"),
    MATRIX3("mat3"),
    MATRIX4("mat4"),
    MATRIX2_DOUBLE("dmat2"),
    MATRIX3_DOUBLE("dmat3"),
    MATRIX4_DOUBLE("dmat4"),

    // Samplers
    SAMPLE_IMAGE_2D("sampler2D"),
    SAMPLE_IMAGE_2D_ARRAY("sampler2DArray");

    // Internal
    private final String glslName;

    UniformType(String glslName) {

        // Internal
        this.glslName = glslName;
    }

    // Utility \\

    private static final Object2ObjectOpenHashMap<String, UniformType> LOOKUP = new Object2ObjectOpenHashMap<>();
    static {
        for (UniformType type : values())
            LOOKUP.put(type.glslName, type);
    }

    // Accessible \\

    public String getGLSLName() {
        return glslName;
    }

    public static UniformType fromString(String glslName) {
        return LOOKUP.get(glslName);
    }
}
