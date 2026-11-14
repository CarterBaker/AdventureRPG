package application.bootstrap.shaderpipeline.uniforms.vectors;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;
import engine.util.mathematics.vectors.Vector2Int;

public final class Vector2IntUniform extends UniformAttributeStruct<Vector2Int> {

    /*
     * GLSL ivec2 uniform. Holds its own vector and copies incoming values
     * into it, so setting it never allocates.
     */

    // Constructor \\

    public Vector2IntUniform() {
        super(UniformType.VECTOR2_INT, new Vector2Int());
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector2IntUniform();
    }

    // Push \\

    @Override
    protected void push(int handle, Vector2Int value) {
        EngineContext.gl20.glUniform2i(handle, value.x, value.y);
    }

    // Accessible \\

    @Override
    protected void applyValue(Vector2Int value) {
        this.value.set(value);
    }

    @Override
    protected void applyObject(Object value) {

        if (value instanceof Vector2Int vector)
            applyValue(vector);
        else
            throwException("Vector2IntUniform expects Vector2Int, got " + value.getClass().getSimpleName());
    }
}
