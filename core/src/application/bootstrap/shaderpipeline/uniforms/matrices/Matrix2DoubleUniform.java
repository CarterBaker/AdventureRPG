package application.bootstrap.shaderpipeline.uniforms.matrices;

import java.nio.FloatBuffer;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import application.core.engine.EngineContext;
import application.core.util.mathematics.matrices.Matrix2Double;

public final class Matrix2DoubleUniform extends UniformAttributeStruct<Matrix2Double> {

    private final FloatBuffer uniformBuffer;

    public Matrix2DoubleUniform() {
        super(UniformType.MATRIX2_DOUBLE, new Matrix2Double());
        this.uniformBuffer = uboBuffer.asFloatBuffer();
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Matrix2DoubleUniform();
    }

    @Override
    protected void push(int handle, Matrix2Double value) {
        uniformBuffer.clear();
        uniformBuffer.put((float) value.val[0]);
        uniformBuffer.put((float) value.val[1]);
        uniformBuffer.put((float) value.val[2]);
        uniformBuffer.put((float) value.val[3]);
        uniformBuffer.flip();
        EngineContext.gl.glUniformMatrix2fv(handle, 1, false, uniformBuffer);
    }

    @Override
    protected void applyValue(Matrix2Double value) {
        this.value.set(value);
    }
}
