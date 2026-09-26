package application.bootstrap.shaderpipeline.uniforms.vectors;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;
import engine.util.mathematics.vectors.Vector4Double;

public final class Vector4DoubleUniform extends UniformAttributeStruct<Vector4Double> {

    /*
     * GLSL dvec4 uniform. Holds its own vector and copies incoming values
     * into it, so setting it never allocates.
     */

    // Constructor \\

    public Vector4DoubleUniform() {
        super(UniformType.VECTOR4_DOUBLE, new Vector4Double());
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector4DoubleUniform();
    }

    // Push \\

    @Override
    protected void push(int handle, Vector4Double value) {
        EngineContext.gl20.glUniform4f(handle, (float) value.x, (float) value.y, (float) value.z, (float) value.w);
    }

    // Accessible \\

    @Override
    protected void applyValue(Vector4Double value) {
        this.value.set(value);
    }

    @Override
    protected void applyObject(Object value) {

        if (value instanceof Vector4Double vector)
            applyValue(vector);
        else
            throwException("Vector4DoubleUniform expects Vector4Double, got " + value.getClass().getSimpleName());
    }
}
