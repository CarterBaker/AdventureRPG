package application.bootstrap.shaderpipeline.uniforms.matrices;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;
import engine.util.mathematics.matrices.Matrix4;

public final class Matrix4Uniform extends UniformAttributeStruct<Object> {

    public Matrix4Uniform() {
        super(UniformType.MATRIX4, new Matrix4());
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Matrix4Uniform();
    }

    @Override
    protected void push(int handle, Object value) {
        if (value instanceof Matrix4 m) {
            try (org.lwjgl.system.MemoryStack stack = org.lwjgl.system.MemoryStack.stackPush()) {
                java.nio.FloatBuffer buf = stack.mallocFloat(16);
                buf.put(m.val).flip();
                EngineContext.gl20.glUniformMatrix4fv(handle, 1, false, buf);
            }
        } else
            throw new IllegalArgumentException("push(Matrix4): got " + value.getClass());
    }

    @Override
    protected void applyValue(Object value) {
        if (value instanceof Matrix4 m)
            ((Matrix4) this.value).set(m);
        else
            throw new IllegalArgumentException("applyValue(Matrix4): got " + value.getClass());
    }

    @Override
    protected void applyObject(Object value) {
        applyValue(value);
    }
}
