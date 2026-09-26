package application.bootstrap.shaderpipeline.uniforms.vectors;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;
import engine.util.mathematics.vectors.Vector2;

public final class Vector2Uniform extends UniformAttributeStruct<Vector2> {

    /*
     * GLSL vec2 uniform. Holds its own vector and copies incoming values
     * into it, so setting it never allocates.
     */

    // Constructor \\

    public Vector2Uniform() {
        super(UniformType.VECTOR2, new Vector2());
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector2Uniform();
    }

    // Push \\

    @Override
    protected void push(int handle, Vector2 value) {
        EngineContext.gl20.glUniform2f(handle, value.x, value.y);
    }

    // Accessible \\

    @Override
    protected void applyValue(Vector2 value) {
        this.value.set(value);
    }

    @Override
    protected void applyObject(Object value) {

        if (value instanceof Vector2 vector)
            applyValue(vector);
        else
            throwException("Vector2Uniform expects Vector2, got " + value.getClass().getSimpleName());
    }
}
