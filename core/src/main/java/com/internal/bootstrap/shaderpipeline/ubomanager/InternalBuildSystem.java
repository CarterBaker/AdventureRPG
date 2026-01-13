package com.internal.bootstrap.shaderpipeline.ubomanager;

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

public final class InternalBuildSystem extends SystemPackage {

    private int nextID = 0;

    public UBOHandle build(UBOData data) {
        int id = nextID++;
        int binding = data.getBinding();
        int gpuHandle = GLSLUtility.createUniformBuffer();

        // Compute std140 buffer layout FIRST
        int totalSize = computeStd140BufferSize(data.getUniforms()); // NEW: get size first

        // Create handle WITH size
        UBOHandle uboHandle = create(UBOHandle.class);
        uboHandle.constructor(
                data.getBlockName(),
                id,
                gpuHandle,
                binding,
                totalSize); // NEW: pass size

        // Populate uniforms (this adds them to the handle)
        populateUniforms(uboHandle, data.getUniforms());

        // Allocate GPU buffer with computed size
        GLSLUtility.allocateUniformBuffer(gpuHandle, totalSize);
        GLSLUtility.bindUniformBufferBase(gpuHandle, binding);

        return uboHandle;
    }

    public void validate(UBOHandle existing, UBOData newData) {

        // Validate binding matches
        if (existing.getBindingPoint() != newData.getBinding()) {
            throwException(
                    "UBO '" + newData.getBlockName() + "' has conflicting bindings: " +
                            "existing=" + existing.getBindingPoint() + ", new=" + newData.getBinding() +
                            ". All declarations of this uniform block must use the same binding point.");
        }

        // Validate structure matches
        ObjectArrayList<UniformData> newUniforms = newData.getUniforms();
        Object2ObjectOpenHashMap<String, Uniform<?>> existingUniforms = existing.getUniforms();

        if (newUniforms.size() != existingUniforms.size()) {
            throwException(
                    "UBO '" + newData.getBlockName() + "' has conflicting structure: " +
                            "different number of uniforms (" + existingUniforms.size() +
                            " vs " + newUniforms.size() + "). " +
                            "Use an #include file to ensure consistency across shaders.");
        }

        // Validate each uniform matches
        for (UniformData uniformData : newUniforms) {
            Uniform<?> existingUniform = existingUniforms.get(uniformData.getUniformName());

            if (existingUniform == null) {
                throwException(
                        "UBO '" + newData.getBlockName() + "' has conflicting structure: " +
                                "uniform '" + uniformData.getUniformName() + "' not found in existing definition. " +
                                "Use an #include file to ensure consistency across shaders.");
            }
        }
    }

    // NEW: Calculate size only (no uniform creation)
    private int computeStd140BufferSize(ObjectArrayList<UniformData> uniformsData) {
        int currentOffset = 0;

        for (UniformData uniformData : uniformsData) {
            int alignment = getStd140Alignment(uniformData);
            int size = getStd140Size(uniformData);

            currentOffset = alignOffset(currentOffset, alignment);
            currentOffset += size;
        }

        return alignOffset(currentOffset, 16);
    }

    // NEW: Populate uniforms into existing handle
    private void populateUniforms(UBOHandle handle, ObjectArrayList<UniformData> uniformsData) {
        int currentOffset = 0;

        for (UniformData uniformData : uniformsData) {
            int alignment = getStd140Alignment(uniformData);
            int size = getStd140Size(uniformData);

            currentOffset = alignOffset(currentOffset, alignment);

            UniformAttribute<?> attribute = createUniformAttribute(uniformData);
            Uniform<?> uniform = new Uniform<>(-1, currentOffset, attribute);

            handle.addUniform(uniformData.getUniformName(), uniform);

            currentOffset += size;
        }
    }

    private int alignOffset(int offset, int alignment) {
        return ((offset + alignment - 1) / alignment) * alignment;
    }

    private int getStd140Alignment(UniformData uniformData) {
        return switch (uniformData.getUniformType()) {
            case FLOAT, INT, BOOL -> 4;
            case VECTOR2, VECTOR2_INT, VECTOR2_BOOLEAN -> 8;
            case VECTOR3, VECTOR3_INT, VECTOR3_BOOLEAN,
                    VECTOR4, VECTOR4_INT, VECTOR4_BOOLEAN ->
                16;
            case MATRIX2 -> 16;
            case MATRIX3 -> 16;
            case MATRIX4 -> 16;
            case DOUBLE -> 8;
            case VECTOR2_DOUBLE -> 8;
            case VECTOR3_DOUBLE, VECTOR4_DOUBLE -> 16;
            case MATRIX2_DOUBLE, MATRIX3_DOUBLE, MATRIX4_DOUBLE -> 16;
            default -> 16;
        };
    }

    private int getStd140Size(UniformData uniformData) {
        int baseSize = switch (uniformData.getUniformType()) {
            case FLOAT, INT, BOOL -> 4;
            case VECTOR2, VECTOR2_INT, VECTOR2_BOOLEAN -> 8;
            case VECTOR3, VECTOR3_INT, VECTOR3_BOOLEAN -> 12;
            case VECTOR4, VECTOR4_INT, VECTOR4_BOOLEAN -> 16;
            case MATRIX2 -> 32; // 2 vec4s in std140
            case MATRIX3 -> 48; // 3 vec4s
            case MATRIX4 -> 64; // 4 vec4s
            case DOUBLE -> 8;
            case VECTOR2_DOUBLE -> 16;
            case VECTOR3_DOUBLE -> 24;
            case VECTOR4_DOUBLE -> 32;
            case MATRIX2_DOUBLE -> 64;
            case MATRIX3_DOUBLE -> 96;
            case MATRIX4_DOUBLE -> 128;
            default -> 16;
        };

        // Handle arrays
        if (uniformData.getCount() > 1) {
            int alignment = getStd140Alignment(uniformData);
            int stride = alignOffset(baseSize, alignment);
            return stride * uniformData.getCount();
        }

        return baseSize;
    }

    private UniformAttribute<?> createUniformAttribute(UniformData uniformData) {

        int getCount = uniformData.getCount();
        boolean isArray = getCount > 1;

        return switch (uniformData.getUniformType()) {

            // Scalars
            case FLOAT -> isArray ? new FloatArrayUniform(getCount) : new FloatUniform();
            case DOUBLE -> isArray ? new DoubleArrayUniform(getCount) : new DoubleUniform();
            case INT -> isArray ? new IntegerArrayUniform(getCount) : new IntegerUniform();
            case BOOL -> isArray ? new BooleanArrayUniform(getCount) : new BooleanUniform();

            // Vectors
            case VECTOR2 -> isArray ? new Vector2ArrayUniform(getCount) : new Vector2Uniform();
            case VECTOR3 -> isArray ? new Vector3ArrayUniform(getCount) : new Vector3Uniform();
            case VECTOR4 -> isArray ? new Vector4ArrayUniform(getCount) : new Vector4Uniform();
            case VECTOR2_DOUBLE -> isArray ? new Vector2DoubleArrayUniform(getCount) : new Vector2DoubleUniform();
            case VECTOR3_DOUBLE -> isArray ? new Vector3DoubleArrayUniform(getCount) : new Vector3DoubleUniform();
            case VECTOR4_DOUBLE -> isArray ? new Vector4DoubleArrayUniform(getCount) : new Vector4DoubleUniform();
            case VECTOR2_INT -> isArray ? new Vector2IntArrayUniform(getCount) : new Vector2IntUniform();
            case VECTOR3_INT -> isArray ? new Vector3IntArrayUniform(getCount) : new Vector3IntUniform();
            case VECTOR4_INT -> isArray ? new Vector4IntArrayUniform(getCount) : new Vector4IntUniform();
            case VECTOR2_BOOLEAN -> isArray ? new Vector2BooleanArrayUniform(getCount) : new Vector2BooleanUniform();
            case VECTOR3_BOOLEAN -> isArray ? new Vector3BooleanArrayUniform(getCount) : new Vector3BooleanUniform();
            case VECTOR4_BOOLEAN -> isArray ? new Vector4BooleanArrayUniform(getCount) : new Vector4BooleanUniform();

            // Matrices
            case MATRIX2 -> isArray ? new Matrix2ArrayUniform(getCount) : new Matrix2Uniform();
            case MATRIX3 -> isArray ? new Matrix3ArrayUniform(getCount) : new Matrix3Uniform();
            case MATRIX4 -> isArray ? new Matrix4ArrayUniform(getCount) : new Matrix4Uniform();
            case MATRIX2_DOUBLE -> isArray ? new Matrix2DoubleArrayUniform(getCount) : new Matrix2DoubleUniform();
            case MATRIX3_DOUBLE -> isArray ? new Matrix3DoubleArrayUniform(getCount) : new Matrix3DoubleUniform();
            case MATRIX4_DOUBLE -> isArray ? new Matrix4DoubleArrayUniform(getCount) : new Matrix4DoubleUniform();

            default -> throwException(
                    "Unsupported uniform type in UBO: " + uniformData.getUniformType());
        };
    }

    // Clone an existing UniformAttribute (used by UBOManager.cloneUBOHandle)
    public UniformAttribute<?> createUniformAttributeClone(UniformAttribute<?> source) {

        // For arrays, extract size from the source
        if (source instanceof FloatArrayUniform fa)
            return new FloatArrayUniform(fa.elementCount());
        if (source instanceof DoubleArrayUniform da)
            return new DoubleArrayUniform(da.elementCount());
        if (source instanceof IntegerArrayUniform ia)
            return new IntegerArrayUniform(ia.elementCount());
        if (source instanceof BooleanArrayUniform ba)
            return new BooleanArrayUniform(ba.elementCount());

        if (source instanceof Vector2ArrayUniform v2a)
            return new Vector2ArrayUniform(v2a.elementCount());
        if (source instanceof Vector3ArrayUniform v3a)
            return new Vector3ArrayUniform(v3a.elementCount());
        if (source instanceof Vector4ArrayUniform v4a)
            return new Vector4ArrayUniform(v4a.elementCount());
        if (source instanceof Vector2DoubleArrayUniform v2da)
            return new Vector2DoubleArrayUniform(v2da.elementCount());
        if (source instanceof Vector3DoubleArrayUniform v3da)
            return new Vector3DoubleArrayUniform(v3da.elementCount());
        if (source instanceof Vector4DoubleArrayUniform v4da)
            return new Vector4DoubleArrayUniform(v4da.elementCount());
        if (source instanceof Vector2IntArrayUniform v2ia)
            return new Vector2IntArrayUniform(v2ia.elementCount());
        if (source instanceof Vector3IntArrayUniform v3ia)
            return new Vector3IntArrayUniform(v3ia.elementCount());
        if (source instanceof Vector4IntArrayUniform v4ia)
            return new Vector4IntArrayUniform(v4ia.elementCount());
        if (source instanceof Vector2BooleanArrayUniform v2ba)
            return new Vector2BooleanArrayUniform(v2ba.elementCount());
        if (source instanceof Vector3BooleanArrayUniform v3ba)
            return new Vector3BooleanArrayUniform(v3ba.elementCount());
        if (source instanceof Vector4BooleanArrayUniform v4ba)
            return new Vector4BooleanArrayUniform(v4ba.elementCount());

        if (source instanceof Matrix2ArrayUniform m2a)
            return new Matrix2ArrayUniform(m2a.elementCount());
        if (source instanceof Matrix3ArrayUniform m3a)
            return new Matrix3ArrayUniform(m3a.elementCount());
        if (source instanceof Matrix4ArrayUniform m4a)
            return new Matrix4ArrayUniform(m4a.elementCount());
        if (source instanceof Matrix2DoubleArrayUniform m2da)
            return new Matrix2DoubleArrayUniform(m2da.elementCount());
        if (source instanceof Matrix3DoubleArrayUniform m3da)
            return new Matrix3DoubleArrayUniform(m3da.elementCount());
        if (source instanceof Matrix4DoubleArrayUniform m4da)
            return new Matrix4DoubleArrayUniform(m4da.elementCount());

        // Non-array types
        if (source instanceof FloatUniform)
            return new FloatUniform();
        if (source instanceof DoubleUniform)
            return new DoubleUniform();
        if (source instanceof IntegerUniform)
            return new IntegerUniform();
        if (source instanceof BooleanUniform)
            return new BooleanUniform();

        if (source instanceof Vector2Uniform)
            return new Vector2Uniform();
        if (source instanceof Vector3Uniform)
            return new Vector3Uniform();
        if (source instanceof Vector4Uniform)
            return new Vector4Uniform();
        if (source instanceof Vector2DoubleUniform)
            return new Vector2DoubleUniform();
        if (source instanceof Vector3DoubleUniform)
            return new Vector3DoubleUniform();
        if (source instanceof Vector4DoubleUniform)
            return new Vector4DoubleUniform();
        if (source instanceof Vector2IntUniform)
            return new Vector2IntUniform();
        if (source instanceof Vector3IntUniform)
            return new Vector3IntUniform();
        if (source instanceof Vector4IntUniform)
            return new Vector4IntUniform();
        if (source instanceof Vector2BooleanUniform)
            return new Vector2BooleanUniform();
        if (source instanceof Vector3BooleanUniform)
            return new Vector3BooleanUniform();
        if (source instanceof Vector4BooleanUniform)
            return new Vector4BooleanUniform();

        if (source instanceof Matrix2Uniform)
            return new Matrix2Uniform();
        if (source instanceof Matrix3Uniform)
            return new Matrix3Uniform();
        if (source instanceof Matrix4Uniform)
            return new Matrix4Uniform();
        if (source instanceof Matrix2DoubleUniform)
            return new Matrix2DoubleUniform();
        if (source instanceof Matrix3DoubleUniform)
            return new Matrix3DoubleUniform();
        if (source instanceof Matrix4DoubleUniform)
            return new Matrix4DoubleUniform();

        return throwException(
                "Cannot clone unknown UniformAttribute type: " + source.getClass().getName());
    }
}