package application.bootstrap.shaderpipeline.uniforms.vectors;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;
import engine.util.mathematics.vectors.Vector4Int;

public final class Vector4IntUniform extends UniformAttributeStruct<Vector4Int> {

    /*
     * GLSL ivec4 uniform. Holds its own vector and copies incoming values
     * into it, so setting it never allocates.
     */

    // Constructor \\

    public Vector4IntUniform() {
        super(UniformType.VECTOR4_INT, new Vector4Int());
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector4IntUniform();
    }

    // Push \\

    @Override
    protected void push(int handle, Vector4Int value) {
        EngineContext.gl20.glUniform4i(handle, value.x, value.y, value.z, value.w);
    }

    // Accessible \\

    @Override
    protected void applyValue(Vector4Int value) {
        this.value.set(value);
    }

    @Override
    protected void applyObject(Object value) {

        if (value instanceof Vector4Int vector)
            applyValue(vector);
        else
            throwException("Vector4IntUniform expects Vector4Int, got " + value.getClass().getSimpleName());
    }
}
