package program.bootstrap.shaderpipeline.uniforms.matrices;

import program.core.app.CoreContext;
import program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import program.bootstrap.shaderpipeline.uniforms.UniformType;
import program.core.util.mathematics.matrices.Matrix4;
import program.core.util.mathematics.matrices.Matrix4Double;

public final class Matrix4DoubleUniform extends UniformAttributeStruct<Matrix4Double> {

    private final Matrix4 uniformBuffer;

    public Matrix4DoubleUniform() {
        super(UniformType.MATRIX4_DOUBLE, new Matrix4Double());
        this.uniformBuffer = new Matrix4();
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Matrix4DoubleUniform();
    }

    @Override
    protected void push(int handle, Matrix4Double value) {
        for (int i = 0; i < 16; i++)
            uniformBuffer.val[i] = (float) value.val[i];
        CoreContext.gl.glUniformMatrix4fv(handle, 1, false, uniformBuffer.val, 0);
    }

    @Override
    protected void applyValue(Matrix4Double value) {
        this.value.set(value);
    }
}
