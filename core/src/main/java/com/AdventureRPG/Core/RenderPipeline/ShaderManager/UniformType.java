package com.AdventureRPG.Core.RenderPipeline.ShaderManager;

import java.util.function.Function;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public enum UniformType {

    // Scalars
    FLOAT("float", GLSLUtility::parseFloatUniform),
    DOUBLE("double", GLSLUtility::parseDoubleUniform),
    INT("int", GLSLUtility::parseIntegerUniform),
    BOOL("bool", GLSLUtility::parseBooleanUniform),

    // Vectors
    VECTOR2("vec2", GLSLUtility::parseVector2Uniform),
    VECTOR3("vec3", GLSLUtility::parseVector3Uniform),
    VECTOR4("vec4", GLSLUtility::parseVector4Uniform),
    VECTOR2_DOUBLE("dvec2", GLSLUtility::parseVector2DoubleUniform),
    VECTOR3_DOUBLE("dvec3", GLSLUtility::parseVector3DoubleUniform),
    VECTOR4_DOUBLE("dvec4", GLSLUtility::parseVector4DoubleUniform),
    VECTOR2_INT("ivec2", GLSLUtility::parseVector2IntUniform),
    VECTOR3_INT("ivec3", GLSLUtility::parseVector3IntUniform),
    VECTOR4_INT("ivec4", GLSLUtility::parseVector4IntUniform),
    VECTOR2_BOOLEAN("bvec2", GLSLUtility::parseVector2BooleanUniform),
    VECTOR3_BOOLEAN("bvec3", GLSLUtility::parseVector3BooleanUniform),
    VECTOR4_BOOLEAN("bvec4", GLSLUtility::parseVector4BooleanUniform),

    // Matrices
    MATRIX2("mat2", GLSLUtility::parseMatrix2Uniform),
    MATRIX3("mat3", GLSLUtility::parseMatrix3Uniform),
    MATRIX4("mat4", GLSLUtility::parseMatrix4Uniform),
    MATRIX2_DOUBLE("dmat2", GLSLUtility::parseMatrix2DoubleUniform),
    MATRIX3_DOUBLE("dmat3", GLSLUtility::parseMatrix3DoubleUniform),
    MATRIX4_DOUBLE("dmat4", GLSLUtility::parseMatrix4DoubleUniform),

    // Samplers
    SAMPLE_IMAGE_2D("sampler2D", GLSLUtility::parseSampleImage2DUniform),
    SAMPLE_IMAGE_2D_ARRAY("sampler2DArray", GLSLUtility::parseSampleImage2DArrayUniform);

    // Internal
    private final String glslName;
    private final Function<String, ?> parser;

    UniformType(
            String glslName,
            Function<String, ?> parser) {

        // Internal
        this.glslName = glslName;
        this.parser = parser;
    }

    // Utility \\

    private static final Object2ObjectOpenHashMap<String, UniformType> LOOKUP = new Object2ObjectOpenHashMap<>();
    static {
        for (UniformType type : values())
            LOOKUP.put(type.glslName, type);
    }

    public Object parse(String input) {
        return parser.apply(input);
    }

    // Accessible \\

    public String getGLSLName() {
        return glslName;
    }

    public static UniformType fromString(String glslName) {
        return LOOKUP.get(glslName);
    }
}
