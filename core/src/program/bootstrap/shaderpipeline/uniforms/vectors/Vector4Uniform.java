package program.bootstrap.shaderpipeline.uniforms.vectors;

import program.core.engine.EngineContext;
import program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import program.bootstrap.shaderpipeline.uniforms.UniformType;
import program.core.util.mathematics.vectors.Vector4;

public final class Vector4Uniform extends UniformAttributeStruct<Object> {

    public Vector4Uniform() {
        super(UniformType.VECTOR4, new Vector4());
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector4Uniform();
    }

    @Override
    protected void push(int handle, Object value) {
        if (value instanceof Vector4 vector)
            EngineContext.gl.glUniform4f(handle, vector.x, vector.y, vector.z, vector.w);
        else
            throw new IllegalArgumentException("push(int, Vector4): got " + value.getClass());
    }

    @Override
    protected void applyValue(Object value) {
        if (value instanceof Vector4 vector)
            ((Vector4) this.value).set(vector);
        else
            throw new IllegalArgumentException("applyValue(Vector4): got " + value.getClass());
    }
}
