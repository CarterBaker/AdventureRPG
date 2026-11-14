package application.bootstrap.shaderpipeline.uniforms.matrices;

import java.nio.FloatBuffer;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;
import engine.root.EngineSetting;
import engine.util.mathematics.matrices.Matrix2;
import engine.util.memory.BufferUtility;

public final class Matrix2Uniform extends UniformAttributeStruct<Matrix2> {

    /*
     * GLSL mat2 uniform. Uploaded through a preallocated direct buffer,
     * so pushes never allocate.
     */

    // Internal
    private final FloatBuffer uniformBuffer;

    // Constructor \\

    public Matrix2Uniform() {
        super(UniformType.MATRIX2, new Matrix2());
        this.uniformBuffer = BufferUtility.newFloatBuffer(EngineSetting.MATRIX2_ELEMENT_COUNT);
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Matrix2Uniform();
    }

    // Push \\

    @Override
    protected void push(int handle, Matrix2 value) {

        uniformBuffer.clear();
        uniformBuffer.put(value.val, 0, EngineSetting.MATRIX2_ELEMENT_COUNT);
        uniformBuffer.flip();

        EngineContext.gl20.glUniformMatrix2fv(handle, 1, false, uniformBuffer);
    }

    // Accessible \\

    @Override
    protected void applyValue(Matrix2 value) {
        this.value.set(value);
    }

    @Override
    protected void applyObject(Object value) {

        if (value instanceof Matrix2 matrix)
            applyValue(matrix);
        else
            throwException("Matrix2Uniform expects Matrix2, got " + value.getClass().getSimpleName());
    }
}
