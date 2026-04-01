package program.bootstrap.shaderpipeline.uniforms.matrices;

import program.core.app.CoreContext;
import program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import program.bootstrap.shaderpipeline.uniforms.UniformType;
import program.core.util.mathematics.matrices.Matrix2;

import java.nio.FloatBuffer;

public final class Matrix2Uniform extends UniformAttributeStruct<Matrix2> {

    private final FloatBuffer uniformBuffer;

    public Matrix2Uniform() {
        super(UniformType.MATRIX2, new Matrix2());
        this.uniformBuffer = uboBuffer.asFloatBuffer();
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Matrix2Uniform();
    }

    @Override
    protected void push(int handle, Matrix2 value) {
        uniformBuffer.clear();
        uniformBuffer.put(value.val[0]);
        uniformBuffer.put(value.val[1]);
        uniformBuffer.put(value.val[2]);
        uniformBuffer.put(value.val[3]);
        uniformBuffer.flip();
        CoreContext.gl.glUniformMatrix2fv(handle, 1, false, uniformBuffer);
    }

    @Override
    protected void applyValue(Matrix2 value) {
        this.value.set(value);
    }
}
