package com.internal.bootstrap.shaderpipeline.ubomanager;

import com.internal.bootstrap.shaderpipeline.ubo.UBOInstance;
import com.internal.bootstrap.shaderpipeline.uniforms.Uniform;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformData;
import com.internal.bootstrap.shaderpipeline.uniforms.matrices.*;
import com.internal.bootstrap.shaderpipeline.uniforms.matrixArrays.*;
import com.internal.bootstrap.shaderpipeline.uniforms.scalarArrays.*;
import com.internal.bootstrap.shaderpipeline.uniforms.scalars.*;
import com.internal.bootstrap.shaderpipeline.uniforms.vectorarrays.*;
import com.internal.bootstrap.shaderpipeline.uniforms.vectors.*;
import com.internal.core.engine.SystemPackage;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/*
 * Constructs UBOHandle objects from UBOData during bootstrap and produces UBOInstance
 * copies for external system use. All std140 layout arithmetic lives here.
 * Validates re-declared blocks for structural consistency against existing handles.
 */
public class InternalBuildSystem extends SystemPackage {

    // Internal
    private int nextID = 0;

    // Build \\

    UBOHandle build(UBOData data, int resolvedBinding) {

        int id = nextID++;
        int gpuHandle = GLSLUtility.createUniformBuffer();
        int totalSize = computeStd140BufferSize(data.getUniforms());

        UBOHandle handle = create(UBOHandle.class);
        handle.constructor(
                data.getBlockName(),
                id,
                gpuHandle,
                resolvedBinding,
                totalSize);

        populateUniforms(handle, data.getUniforms());

        GLSLUtility.allocateUniformBuffer(gpuHandle, totalSize);
        GLSLUtility.bindUniformBufferBase(gpuHandle, resolvedBinding);

        return handle;
    }

    UBOInstance cloneFromHandle(UBOHandle source) {

        int newGpuHandle = GLSLUtility.createUniformBuffer();
        GLSLUtility.allocateUniformBuffer(newGpuHandle, source.getTotalSizeBytes());
        GLSLUtility.bindUniformBufferBase(newGpuHandle, source.getBindingPoint());

        UBOInstance instance = create(UBOInstance.class);
        instance.constructor(
                source.getBufferName(),
                newGpuHandle,
                source.getBindingPoint(),
                source.getTotalSizeBytes());

        Object2ObjectOpenHashMap<String, Uniform<?>> sourceUniforms = source.getUniforms();
        String[] keys = sourceUniforms.keySet().toArray(new String[0]);

        for (int i = 0; i < keys.length; i++) {
            Uniform<?> sourceUniform = sourceUniforms.get(keys[i]);
            UniformAttribute<?> newAttr = sourceUniform.attribute().createDefault();
            instance.addUniform(keys[i], new Uniform<>(-1, sourceUniform.offset, newAttr));
        }

        return instance;
    }

    // Validate \\

    void validate(UBOHandle existing, UBOData newData) {

        if (newData.getBinding() != UBOData.UNSPECIFIED_BINDING &&
                existing.getBindingPoint() != newData.getBinding())
            throwException(
                    "UBO '" + newData.getBlockName() + "' has conflicting binding: " +
                            "existing=" + existing.getBindingPoint() +
                            ", requested=" + newData.getBinding() +
                            ". All declarations of this block must use the same binding point.");

        ObjectArrayList<UniformData> newUniforms = newData.getUniforms();
        Object2ObjectOpenHashMap<String, Uniform<?>> existingUniforms = existing.getUniforms();

        if (newUniforms.size() != existingUniforms.size())
            throwException(
                    "UBO '" + newData.getBlockName() + "' has conflicting structure: " +
                            existingUniforms.size() + " vs " + newUniforms.size() + " uniforms. " +
                            "Use an #include to ensure consistent block definitions across shaders.");

        for (int i = 0; i < newUniforms.size(); i++) {
            if (existingUniforms.get(newUniforms.get(i).getUniformName()) == null)
                throwException(
                        "UBO '" + newData.getBlockName() + "' has conflicting structure: uniform '" +
                                newUniforms.get(i).getUniformName() +
                                "' not found in existing definition. " +
                                "Use an #include to ensure consistent block definitions across shaders.");
        }
    }

    // Std140 Layout \\

    private int computeStd140BufferSize(ObjectArrayList<UniformData> uniformsData) {

        int offset = 0;

        for (int i = 0; i < uniformsData.size(); i++) {
            UniformData ud = uniformsData.get(i);
            offset = align(offset, getStd140Alignment(ud));
            offset += getStd140Size(ud);
        }

        return align(offset, 16);
    }

    private void populateUniforms(UBOHandle handle, ObjectArrayList<UniformData> uniformsData) {

        int offset = 0;

        for (int i = 0; i < uniformsData.size(); i++) {
            UniformData ud = uniformsData.get(i);
            offset = align(offset, getStd140Alignment(ud));

            handle.addUniform(
                    ud.getUniformName(),
                    new Uniform<>(-1, offset, createUniformAttribute(ud)));

            offset += getStd140Size(ud);
        }
    }

    private int align(int offset, int alignment) {
        return ((offset + alignment - 1) / alignment) * alignment;
    }

    /*
     * std140 base alignment rules (OpenGL spec §7.6.2):
     *
     * Scalars:
     * float / int / bool → 4
     * double → 8
     *
     * Vectors (N = sizeof base type):
     * 2-component → 2N (vec2=8, dvec2=16)
     * 3-component → 4N (vec3=16, dvec3=32)
     * 4-component → 4N (vec4=16, dvec4=32)
     *
     * Arrays and matrices: always align to 16.
     */
    private int getStd140Alignment(UniformData ud) {

        if (ud.getCount() > 1)
            return 16;

        return switch (ud.getUniformType()) {
            case FLOAT, INT, BOOL -> 4;
            case DOUBLE -> 8;
            case VECTOR2,
                    VECTOR2_INT,
                    VECTOR2_BOOLEAN ->
                8;
            case VECTOR2_DOUBLE -> 16;
            case VECTOR3,
                    VECTOR3_INT,
                    VECTOR3_BOOLEAN,
                    VECTOR4,
                    VECTOR4_INT,
                    VECTOR4_BOOLEAN ->
                16;
            case VECTOR3_DOUBLE,
                    VECTOR4_DOUBLE ->
                32;
            default -> 16;
        };
    }

    /*
     * std140 element size in bytes. Arrays round each element up to a 16-byte
     * stride.
     */
    private int getStd140Size(UniformData ud) {

        int base = switch (ud.getUniformType()) {
            case FLOAT, INT, BOOL -> 4;
            case DOUBLE -> 8;
            case VECTOR2, VECTOR2_INT,
                    VECTOR2_BOOLEAN ->
                8;
            case VECTOR3, VECTOR3_INT,
                    VECTOR3_BOOLEAN ->
                12;
            case VECTOR4, VECTOR4_INT,
                    VECTOR4_BOOLEAN ->
                16;
            case MATRIX2 -> 32;
            case MATRIX3 -> 48;
            case MATRIX4 -> 64;
            case VECTOR2_DOUBLE -> 16;
            case VECTOR3_DOUBLE -> 24;
            case VECTOR4_DOUBLE -> 32;
            case MATRIX2_DOUBLE -> 64;
            case MATRIX3_DOUBLE -> 96;
            case MATRIX4_DOUBLE -> 128;
            default -> 16;
        };

        if (ud.getCount() > 1)
            return align(base, 16) * ud.getCount();

        return base;
    }

    private UniformAttribute<?> createUniformAttribute(UniformData ud) {

        int count = ud.getCount();
        boolean isArray = count > 1;

        return switch (ud.getUniformType()) {
            case FLOAT -> isArray ? new FloatArrayUniform(count) : new FloatUniform();
            case DOUBLE -> isArray ? new DoubleArrayUniform(count) : new DoubleUniform();
            case INT -> isArray ? new IntegerArrayUniform(count) : new IntegerUniform();
            case BOOL -> isArray ? new BooleanArrayUniform(count) : new BooleanUniform();
            case VECTOR2 -> isArray ? new Vector2ArrayUniform(count) : new Vector2Uniform();
            case VECTOR3 -> isArray ? new Vector3ArrayUniform(count) : new Vector3Uniform();
            case VECTOR4 -> isArray ? new Vector4ArrayUniform(count) : new Vector4Uniform();
            case VECTOR2_DOUBLE -> isArray ? new Vector2DoubleArrayUniform(count) : new Vector2DoubleUniform();
            case VECTOR3_DOUBLE -> isArray ? new Vector3DoubleArrayUniform(count) : new Vector3DoubleUniform();
            case VECTOR4_DOUBLE -> isArray ? new Vector4DoubleArrayUniform(count) : new Vector4DoubleUniform();
            case VECTOR2_INT -> isArray ? new Vector2IntArrayUniform(count) : new Vector2IntUniform();
            case VECTOR3_INT -> isArray ? new Vector3IntArrayUniform(count) : new Vector3IntUniform();
            case VECTOR4_INT -> isArray ? new Vector4IntArrayUniform(count) : new Vector4IntUniform();
            case VECTOR2_BOOLEAN -> isArray ? new Vector2BooleanArrayUniform(count) : new Vector2BooleanUniform();
            case VECTOR3_BOOLEAN -> isArray ? new Vector3BooleanArrayUniform(count) : new Vector3BooleanUniform();
            case VECTOR4_BOOLEAN -> isArray ? new Vector4BooleanArrayUniform(count) : new Vector4BooleanUniform();
            case MATRIX2 -> isArray ? new Matrix2ArrayUniform(count) : new Matrix2Uniform();
            case MATRIX3 -> isArray ? new Matrix3ArrayUniform(count) : new Matrix3Uniform();
            case MATRIX4 -> isArray ? new Matrix4ArrayUniform(count) : new Matrix4Uniform();
            case MATRIX2_DOUBLE -> isArray ? new Matrix2DoubleArrayUniform(count) : new Matrix2DoubleUniform();
            case MATRIX3_DOUBLE -> isArray ? new Matrix3DoubleArrayUniform(count) : new Matrix3DoubleUniform();
            case MATRIX4_DOUBLE -> isArray ? new Matrix4DoubleArrayUniform(count) : new Matrix4DoubleUniform();
            default -> throwException("Unsupported uniform type in UBO: " + ud.getUniformType());
        };
    }
}