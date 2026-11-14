package application.bootstrap.shaderpipeline.uniforms.vectors;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;
import engine.util.mathematics.vectors.Vector4Boolean;

public final class Vector4BooleanUniform extends UniformAttributeStruct<Vector4Boolean> {

    /*
     * GLSL bvec4 uniform. Holds its own vector and copies incoming values
     * into it, so setting it never allocates.
     */

    // Constructor \\

    public Vector4BooleanUniform() {
        super(UniformType.VECTOR4_BOOLEAN, new Vector4Boolean());
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector4BooleanUniform();
    }

    // Push \\

    @Override
    protected void push(int handle, Vector4Boolean value) {
        EngineContext.gl20.glUniform4i(handle, value.x ? 1 : 0, value.y ? 1 : 0, value.z ? 1 : 0, value.w ? 1 : 0);
    }

    // Accessible \\

    @Override
    protected void applyValue(Vector4Boolean value) {
        this.value.set(value);
    }

    @Override
    protected void applyObject(Object value) {

        if (value instanceof Vector4Boolean vector)
            applyValue(vector);
        else
            throwException("Vector4BooleanUniform expects Vector4Boolean, got " + value.getClass().getSimpleName());
    }
}
