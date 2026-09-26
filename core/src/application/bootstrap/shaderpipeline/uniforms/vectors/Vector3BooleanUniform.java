package application.bootstrap.shaderpipeline.uniforms.vectors;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;
import engine.util.mathematics.vectors.Vector3Boolean;

public final class Vector3BooleanUniform extends UniformAttributeStruct<Vector3Boolean> {

    /*
     * GLSL bvec3 uniform. Holds its own vector and copies incoming values
     * into it, so setting it never allocates.
     */

    // Constructor \\

    public Vector3BooleanUniform() {
        super(UniformType.VECTOR3_BOOLEAN, new Vector3Boolean());
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector3BooleanUniform();
    }

    // Push \\

    @Override
    protected void push(int handle, Vector3Boolean value) {
        EngineContext.gl20.glUniform3i(handle, value.x ? 1 : 0, value.y ? 1 : 0, value.z ? 1 : 0);
    }

    // Accessible \\

    @Override
    protected void applyValue(Vector3Boolean value) {
        this.value.set(value);
    }

    @Override
    protected void applyObject(Object value) {

        if (value instanceof Vector3Boolean vector)
            applyValue(vector);
        else
            throwException("Vector3BooleanUniform expects Vector3Boolean, got " + value.getClass().getSimpleName());
    }
}
