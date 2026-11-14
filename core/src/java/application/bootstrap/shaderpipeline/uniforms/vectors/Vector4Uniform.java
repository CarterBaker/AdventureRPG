package application.bootstrap.shaderpipeline.uniforms.vectors;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;
import engine.util.mathematics.vectors.Vector4;

public final class Vector4Uniform extends UniformAttributeStruct<Vector4> {

    /*
     * GLSL vec4 uniform. Holds its own vector and copies incoming values
     * into it, so setting it never allocates.
     */

    // Constructor \\

    public Vector4Uniform() {
        super(UniformType.VECTOR4, new Vector4());
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector4Uniform();
    }

    // Push \\

    @Override
    protected void push(int handle, Vector4 value) {
        EngineContext.gl20.glUniform4f(handle, value.x, value.y, value.z, value.w);
    }

    // Accessible \\

    @Override
    protected void applyValue(Vector4 value) {
        this.value.set(value);
    }

    @Override
    protected void applyObject(Object value) {

        if (value instanceof Vector4 vector)
            applyValue(vector);
        else
            throwException("Vector4Uniform expects Vector4, got " + value.getClass().getSimpleName());
    }
}
