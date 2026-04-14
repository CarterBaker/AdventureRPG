package application.bootstrap.shaderpipeline.uniforms.vectors;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;
import engine.util.mathematics.vectors.Vector2;

public final class Vector2Uniform extends UniformAttributeStruct<Object> {

    public Vector2Uniform() {
        super(UniformType.VECTOR2, new Vector2());
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector2Uniform();
    }

    @Override
    protected void push(int handle, Object value) {
        if (value instanceof Vector2 vector)
            EngineContext.gl20.glUniform2f(handle, vector.x, vector.y);
        else
            throw new IllegalArgumentException("push(int, Vector2): got " + value.getClass());
    }

    @Override
    protected void applyValue(Object value) {
        if (value instanceof Vector2 vector)
            ((Vector2) this.value).set(vector);
        else
            throw new IllegalArgumentException("applyValue(Vector2): got " + value.getClass());
    }
}
