package application.bootstrap.shaderpipeline.uniforms.matrices;

import java.nio.FloatBuffer;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;
import engine.root.EngineSetting;
import engine.util.mathematics.matrices.Matrix3;
import engine.util.memory.BufferUtility;

public final class Matrix3Uniform extends UniformAttributeStruct<Matrix3> {

    /*
     * GLSL mat3 uniform. Uploaded through a preallocated direct buffer,
     * so pushes never allocate.
     */

    // Internal
    private final FloatBuffer uniformBuffer;

    // Constructor \\

    public Matrix3Uniform() {
        super(UniformType.MATRIX3, new Matrix3());
        this.uniformBuffer = BufferUtility.newFloatBuffer(EngineSetting.MATRIX3_ELEMENT_COUNT);
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Matrix3Uniform();
    }

    // Push \\

    @Override
    protected void push(int handle, Matrix3 value) {

        uniformBuffer.clear();
        uniformBuffer.put(value.val, 0, EngineSetting.MATRIX3_ELEMENT_COUNT);
        uniformBuffer.flip();

        EngineContext.gl20.glUniformMatrix3fv(handle, 1, false, uniformBuffer);
    }

    // Accessible \\

    @Override
    protected void applyValue(Matrix3 value) {
        this.value.set(value);
    }

    @Override
    protected void applyObject(Object value) {

        if (value instanceof Matrix3 matrix)
            applyValue(matrix);
        else
            throwException("Matrix3Uniform expects Matrix3, got " + value.getClass().getSimpleName());
    }
}
