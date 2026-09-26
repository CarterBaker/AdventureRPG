package application.bootstrap.shaderpipeline.uniforms.matrices;

import java.nio.FloatBuffer;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;
import engine.root.EngineSetting;
import engine.util.mathematics.matrices.Matrix4;
import engine.util.memory.BufferUtility;

public final class Matrix4Uniform extends UniformAttributeStruct<Matrix4> {

    /*
     * GLSL mat4 uniform. Uploaded through a preallocated direct buffer,
     * so pushes never allocate.
     */

    // Internal
    private final FloatBuffer uniformBuffer;

    // Constructor \\

    public Matrix4Uniform() {
        super(UniformType.MATRIX4, new Matrix4());
        this.uniformBuffer = BufferUtility.newFloatBuffer(EngineSetting.MATRIX4_ELEMENT_COUNT);
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Matrix4Uniform();
    }

    // Push \\

    @Override
    protected void push(int handle, Matrix4 value) {

        uniformBuffer.clear();
        uniformBuffer.put(value.val, 0, EngineSetting.MATRIX4_ELEMENT_COUNT);
        uniformBuffer.flip();

        EngineContext.gl20.glUniformMatrix4fv(handle, 1, false, uniformBuffer);
    }

    // Accessible \\

    @Override
    protected void applyValue(Matrix4 value) {
        this.value.set(value);
    }

    @Override
    protected void applyObject(Object value) {

        if (value instanceof Matrix4 matrix)
            applyValue(matrix);
        else
            throwException("Matrix4Uniform expects Matrix4, got " + value.getClass().getSimpleName());
    }
}
