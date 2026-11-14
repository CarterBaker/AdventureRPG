package application.bootstrap.shaderpipeline.uniforms.vectors;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;
import engine.util.mathematics.vectors.Vector2Double;

public final class Vector2DoubleUniform extends UniformAttributeStruct<Vector2Double> {

    /*
     * GLSL dvec2 uniform. Holds its own vector and copies incoming values
     * into it, so setting it never allocates.
     */

    // Constructor \\

    public Vector2DoubleUniform() {
        super(UniformType.VECTOR2_DOUBLE, new Vector2Double());
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector2DoubleUniform();
    }

    // Push \\

    @Override
    protected void push(int handle, Vector2Double value) {
        EngineContext.gl20.glUniform2f(handle, (float) value.x, (float) value.y);
    }

    // Accessible \\

    @Override
    protected void applyValue(Vector2Double value) {
        this.value.set(value);
    }

    @Override
    protected void applyObject(Object value) {

        if (value instanceof Vector2Double vector)
            applyValue(vector);
        else
            throwException("Vector2DoubleUniform expects Vector2Double, got " + value.getClass().getSimpleName());
    }
}
