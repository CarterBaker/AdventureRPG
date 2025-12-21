package com.AdventureRPG.core.shaders.ubomanager;

import com.AdventureRPG.core.engine.SystemFrame;
import com.AdventureRPG.core.shaders.uniforms.Uniform;
import com.AdventureRPG.core.shaders.uniforms.UniformAttribute;
import com.AdventureRPG.core.shaders.uniforms.UniformData;
import com.AdventureRPG.core.shaders.uniforms.matrices.*;
import com.AdventureRPG.core.shaders.uniforms.matrixArrays.*;
import com.AdventureRPG.core.shaders.uniforms.scalarArrays.*;
import com.AdventureRPG.core.shaders.uniforms.scalars.*;
import com.AdventureRPG.core.shaders.uniforms.vectorarrays.*;
import com.AdventureRPG.core.shaders.uniforms.vectors.*;
import com.AdventureRPG.core.util.Exceptions.GraphicException;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public final class InternalBuildSystem extends SystemFrame {

    private int nextID = 0;

    public UBOHandle build(UBOData data) {

        int id = nextID++;
        int binding = data.binding();

        // Create GPU buffer
        int gpuHandle = GLSLUtility.createUniformBuffer();

        // Create handle
        UBOHandle handle = new UBOHandle(
                data.blockName(),
                id,
                gpuHandle,
                binding);

        // Compute std140 buffer and populate uniforms
        int totalSize = computeStd140Buffer(handle, data.getUniforms());

        // Allocate GPU buffer with computed size
        GLSLUtility.allocateUniformBuffer(gpuHandle, totalSize);

        // Bind to binding point
        GLSLUtility.bindUniformBufferBase(gpuHandle, binding);

        return handle;
    }

    public void validate(UBOHandle existing, UBOData newData) {

        // Validate binding matches
        if (existing.bindingPoint != newData.binding()) {
            throw new GraphicException.ShaderProgramException(
                    "UBO '" + newData.blockName() + "' has conflicting bindings: " +
                            "existing=" + existing.bindingPoint + ", new=" + newData.binding() +
                            ". All declarations of this uniform block must use the same binding point.");
        }

        // Validate structure matches
        ObjectArrayList<UniformData> newUniforms = newData.getUniforms();
        Object2ObjectOpenHashMap<String, Uniform<?>> existingUniforms = existing.getUniforms();

        if (newUniforms.size() != existingUniforms.size()) {
            throw new GraphicException.ShaderProgramException(
                    "UBO '" + newData.blockName() + "' has conflicting structure: " +
                            "different number of uniforms (" + existingUniforms.size() +
                            " vs " + newUniforms.size() + "). " +
                            "Use an #include file to ensure consistency across shaders.");
        }

        // Validate each uniform matches
        for (UniformData uniformData : newUniforms) {
            Uniform<?> existingUniform = existingUniforms.get(uniformData.uniformName());

            if (existingUniform == null) {
                throw new GraphicException.ShaderProgramException(
                        "UBO '" + newData.blockName() + "' has conflicting structure: " +
                                "uniform '" + uniformData.uniformName() + "' not found in existing definition. " +
                                "Use an #include file to ensure consistency across shaders.");
            }
        }
    }

    private int computeStd140Buffer(
            UBOHandle handle,
            ObjectArrayList<UniformData> uniformsData) {

        int currentOffset = 0;

        for (UniformData uniformData : uniformsData) {

            int alignment = getStd140Alignment(uniformData);
            int size = getStd140Size(uniformData);

            // Align current offset
            currentOffset = alignOffset(currentOffset, alignment);

            // Create uniform attribute
            UniformAttribute<?> attribute = createUniformAttribute(uniformData);

            // Create uniform with offset (no handle needed for UBO uniforms)
            Uniform<?> uniform = new Uniform<>(
                    -1, // No handle for UBO uniforms
                    currentOffset,
                    attribute);

            // Add to handle
            handle.addUniform(uniformData.uniformName(), uniform);

            // Advance offset
            currentOffset += size;
        }

        // Final alignment to 16 bytes (std140 requirement)
        return alignOffset(currentOffset, 16);
    }

    private int alignOffset(int offset, int alignment) {
        return ((offset + alignment - 1) / alignment) * alignment;
    }

    private int getStd140Alignment(UniformData uniformData) {
        return switch (uniformData.uniformType()) {
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
        int baseSize = switch (uniformData.uniformType()) {
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
        if (uniformData.count() > 1) {
            int alignment = getStd140Alignment(uniformData);
            int stride = alignOffset(baseSize, alignment);
            return stride * uniformData.count();
        }

        return baseSize;
    }

    private UniformAttribute<?> createUniformAttribute(UniformData uniformData) {

        int count = uniformData.count();
        boolean isArray = count > 1;

        return switch (uniformData.uniformType()) {

            // Scalars
            case FLOAT -> isArray ? new FloatArrayUniform(count) : new FloatUniform();
            case DOUBLE -> isArray ? new DoubleArrayUniform(count) : new DoubleUniform();
            case INT -> isArray ? new IntegerArrayUniform(count) : new IntegerUniform();
            case BOOL -> isArray ? new BooleanArrayUniform(count) : new BooleanUniform();

            // Vectors
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

            // Matrices
            case MATRIX2 -> isArray ? new Matrix2ArrayUniform(count) : new Matrix2Uniform();
            case MATRIX3 -> isArray ? new Matrix3ArrayUniform(count) : new Matrix3Uniform();
            case MATRIX4 -> isArray ? new Matrix4ArrayUniform(count) : new Matrix4Uniform();
            case MATRIX2_DOUBLE -> isArray ? new Matrix2DoubleArrayUniform(count) : new Matrix2DoubleUniform();
            case MATRIX3_DOUBLE -> isArray ? new Matrix3DoubleArrayUniform(count) : new Matrix3DoubleUniform();
            case MATRIX4_DOUBLE -> isArray ? new Matrix4DoubleArrayUniform(count) : new Matrix4DoubleUniform();

            default -> throw new GraphicException.ShaderProgramException(
                    "Unsupported uniform type in UBO: " + uniformData.uniformType());
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

        throw new GraphicException.ShaderProgramException(
                "Cannot clone unknown UniformAttribute type: " + source.getClass().getName());
    }
}