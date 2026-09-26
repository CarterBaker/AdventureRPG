package application.bootstrap.shaderpipeline.uniforms.matrices;

import java.nio.FloatBuffer;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;
import engine.root.EngineSetting;
import engine.util.mathematics.matrices.Matrix2Double;
import engine.util.memory.BufferUtility;

public final class Matrix2DoubleUniform extends UniformAttributeStruct<Matrix2Double> {

    /*
     * GLSL dmat2 uniform. Uploaded through a preallocated direct buffer,
     * so pushes never allocate.
     */

    // Internal
    private final FloatBuffer uniformBuffer;

    // Constructor \\

    public Matrix2DoubleUniform() {
        super(UniformType.MATRIX2_DOUBLE, new Matrix2Double());
        this.uniformBuffer = BufferUtility.newFloatBuffer(EngineSetting.MATRIX2_ELEMENT_COUNT);
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Matrix2DoubleUniform();
    }

    // Push \\

    @Override
    protected void push(int handle, Matrix2Double value) {

        uniformBuffer.clear();
        for (int i = 0; i < EngineSetting.MATRIX2_ELEMENT_COUNT; i++)
            uniformBuffer.put((float) value.val[i]);
        uniformBuffer.flip();

        EngineContext.gl20.glUniformMatrix2fv(handle, 1, false, uniformBuffer);
    }

    // Accessible \\

    @Override
    protected void applyValue(Matrix2Double value) {
        this.value.set(value);
    }

    @Override
    protected void applyObject(Object value) {

        if (value instanceof Matrix2Double matrix)
            applyValue(matrix);
        else
            throwException("Matrix2DoubleUniform expects Matrix2Double, got " + value.getClass().getSimpleName());
    }
}
