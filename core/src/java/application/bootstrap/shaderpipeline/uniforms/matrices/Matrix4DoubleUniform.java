package application.bootstrap.shaderpipeline.uniforms.matrices;

import java.nio.FloatBuffer;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;
import engine.root.EngineSetting;
import engine.util.mathematics.matrices.Matrix4Double;
import engine.util.memory.BufferUtility;

public final class Matrix4DoubleUniform extends UniformAttributeStruct<Matrix4Double> {

    /*
     * GLSL dmat4 uniform. Uploaded through a preallocated direct buffer,
     * so pushes never allocate.
     */

    // Internal
    private final FloatBuffer uniformBuffer;

    // Constructor \\

    public Matrix4DoubleUniform() {
        super(UniformType.MATRIX4_DOUBLE, new Matrix4Double());
        this.uniformBuffer = BufferUtility.newFloatBuffer(EngineSetting.MATRIX4_ELEMENT_COUNT);
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Matrix4DoubleUniform();
    }

    // Push \\

    @Override
    protected void push(int handle, Matrix4Double value) {

        uniformBuffer.clear();
        for (int i = 0; i < EngineSetting.MATRIX4_ELEMENT_COUNT; i++)
            uniformBuffer.put((float) value.val[i]);
        uniformBuffer.flip();

        EngineContext.gl20.glUniformMatrix4fv(handle, 1, false, uniformBuffer);
    }

    // Accessible \\

    @Override
    protected void applyValue(Matrix4Double value) {
        this.value.set(value);
    }

    @Override
    protected void applyObject(Object value) {

        if (value instanceof Matrix4Double matrix)
            applyValue(matrix);
        else
            throwException("Matrix4DoubleUniform expects Matrix4Double, got " + value.getClass().getSimpleName());
    }
}
