package application.bootstrap.shaderpipeline.uniforms.matrices;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import application.core.engine.EngineContext;
import application.core.util.mathematics.matrices.Matrix4;
import application.core.util.mathematics.matrices.Matrix4Double;

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
        try (org.lwjgl.system.MemoryStack stack = org.lwjgl.system.MemoryStack.stackPush()) {
            java.nio.FloatBuffer buf = stack.mallocFloat(16);
            buf.put(uniformBuffer.val).flip();
            EngineContext.gl.glUniformMatrix4fv(handle, 1, false, buf);
        }
    }

    @Override
    protected void applyValue(Matrix4Double value) {
        this.value.set(value);
    }
}
