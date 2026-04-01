package program.bootstrap.shaderpipeline.uniforms.matrices;

import program.core.app.CoreContext;
import program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import program.bootstrap.shaderpipeline.uniforms.UniformType;
import program.core.util.mathematics.matrices.Matrix4;

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
        if (value instanceof Matrix4 m)
            CoreContext.gl.glUniformMatrix4fv(handle, 1, false, m.val, 0);
        else
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
