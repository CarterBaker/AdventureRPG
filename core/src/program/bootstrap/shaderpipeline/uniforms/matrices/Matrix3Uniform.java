package program.bootstrap.shaderpipeline.uniforms.matrices;

import program.core.engine.EngineContext;
import program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import program.bootstrap.shaderpipeline.uniforms.UniformType;
import program.core.util.mathematics.matrices.Matrix3;

public final class Matrix3Uniform extends UniformAttributeStruct<Object> {

    public Matrix3Uniform() {
        super(UniformType.MATRIX3, new Matrix3());
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Matrix3Uniform();
    }

    @Override
    protected void push(int handle, Object value) {
        if (value instanceof Matrix3 m) {
            try (org.lwjgl.system.MemoryStack stack = org.lwjgl.system.MemoryStack.stackPush()) {
                java.nio.FloatBuffer buf = stack.mallocFloat(9);
                buf.put(m.val).flip();
                EngineContext.gl.glUniformMatrix3fv(handle, 1, false, buf);
            }
        } else
            throw new IllegalArgumentException("push(Matrix3): got " + value.getClass());
    }

    @Override
    protected void applyValue(Object value) {
        if (value instanceof Matrix3 m)
            ((Matrix3) this.value).set(m);
        else
            throw new IllegalArgumentException("applyValue(Matrix3): got " + value.getClass());
    }

    @Override
    protected void applyObject(Object value) {
        applyValue(value);
    }
}
