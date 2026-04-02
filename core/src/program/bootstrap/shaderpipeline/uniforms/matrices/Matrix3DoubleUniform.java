package program.bootstrap.shaderpipeline.uniforms.matrices;

import program.core.app.CoreContext;
import program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import program.bootstrap.shaderpipeline.uniforms.UniformType;
import program.core.util.mathematics.matrices.Matrix3;
import program.core.util.mathematics.matrices.Matrix3Double;

public final class Matrix3DoubleUniform extends UniformAttributeStruct<Matrix3Double> {

    private final Matrix3 uniformBuffer;

    public Matrix3DoubleUniform() {
        super(UniformType.MATRIX3_DOUBLE, new Matrix3Double());
        this.uniformBuffer = new Matrix3();
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Matrix3DoubleUniform();
    }

    @Override
    protected void push(int handle, Matrix3Double value) {
        for (int i = 0; i < 9; i++)
            uniformBuffer.val[i] = (float) value.val[i];
        try (org.lwjgl.system.MemoryStack stack = org.lwjgl.system.MemoryStack.stackPush()) {
            java.nio.FloatBuffer buf = stack.mallocFloat(9);
            buf.put(uniformBuffer.val).flip();
            CoreContext.gl.glUniformMatrix3fv(handle, 1, false, buf);
        }
    }

    @Override
    protected void applyValue(Matrix3Double value) {
        this.value.set(value);
    }
}
