package application.bootstrap.shaderpipeline.uniforms.vectors;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;
import engine.util.mathematics.vectors.Vector3Double;

public final class Vector3DoubleUniform extends UniformAttributeStruct<Vector3Double> {

    /*
     * GLSL dvec3 uniform. Holds its own vector and copies incoming values
     * into it, so setting it never allocates.
     */

    // Constructor \\

    public Vector3DoubleUniform() {
        super(UniformType.VECTOR3_DOUBLE, new Vector3Double());
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector3DoubleUniform();
    }

    // Push \\

    @Override
    protected void push(int handle, Vector3Double value) {
        EngineContext.gl20.glUniform3f(handle, (float) value.x, (float) value.y, (float) value.z);
    }

    // Accessible \\

    @Override
    protected void applyValue(Vector3Double value) {
        this.value.set(value);
    }

    @Override
    protected void applyObject(Object value) {

        if (value instanceof Vector3Double vector)
            applyValue(vector);
        else
            throwException("Vector3DoubleUniform expects Vector3Double, got " + value.getClass().getSimpleName());
    }
}
