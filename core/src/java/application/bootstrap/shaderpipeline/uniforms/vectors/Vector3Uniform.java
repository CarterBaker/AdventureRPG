package application.bootstrap.shaderpipeline.uniforms.vectors;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;
import engine.util.mathematics.vectors.Vector3;

public final class Vector3Uniform extends UniformAttributeStruct<Vector3> {

    /*
     * GLSL vec3 uniform. Holds its own vector and copies incoming values
     * into it, so setting it never allocates.
     */

    // Constructor \\

    public Vector3Uniform() {
        super(UniformType.VECTOR3, new Vector3());
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector3Uniform();
    }

    // Push \\

    @Override
    protected void push(int handle, Vector3 value) {
        EngineContext.gl20.glUniform3f(handle, value.x, value.y, value.z);
    }

    // Accessible \\

    @Override
    protected void applyValue(Vector3 value) {
        this.value.set(value);
    }

    @Override
    protected void applyObject(Object value) {

        if (value instanceof Vector3 vector)
            applyValue(vector);
        else
            throwException("Vector3Uniform expects Vector3, got " + value.getClass().getSimpleName());
    }
}
