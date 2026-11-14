package application.bootstrap.shaderpipeline.uniforms.matrices;

import java.nio.FloatBuffer;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;
import engine.root.EngineSetting;
import engine.util.mathematics.matrices.Matrix3Double;
import engine.util.memory.BufferUtility;

public final class Matrix3DoubleUniform extends UniformAttributeStruct<Matrix3Double> {

    /*
     * GLSL dmat3 uniform. Uploaded through a preallocated direct buffer,
     * so pushes never allocate.
     */

    // Internal
    private final FloatBuffer uniformBuffer;

    // Constructor \\

    public Matrix3DoubleUniform() {
        super(UniformType.MATRIX3_DOUBLE, new Matrix3Double());
        this.uniformBuffer = BufferUtility.newFloatBuffer(EngineSetting.MATRIX3_ELEMENT_COUNT);
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Matrix3DoubleUniform();
    }

    // Push \\

    @Override
    protected void push(int handle, Matrix3Double value) {

        uniformBuffer.clear();
        for (int i = 0; i < EngineSetting.MATRIX3_ELEMENT_COUNT; i++)
            uniformBuffer.put((float) value.val[i]);
        uniformBuffer.flip();

        EngineContext.gl20.glUniformMatrix3fv(handle, 1, false, uniformBuffer);
    }

    // Accessible \\

    @Override
    protected void applyValue(Matrix3Double value) {
        this.value.set(value);
    }

    @Override
    protected void applyObject(Object value) {

        if (value instanceof Matrix3Double matrix)
            applyValue(matrix);
        else
            throwException("Matrix3DoubleUniform expects Matrix3Double, got " + value.getClass().getSimpleName());
    }
}
