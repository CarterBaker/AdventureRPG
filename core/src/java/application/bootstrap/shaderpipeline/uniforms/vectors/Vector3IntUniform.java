package application.bootstrap.shaderpipeline.uniforms.vectors;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;
import engine.util.mathematics.vectors.Vector3Int;

public final class Vector3IntUniform extends UniformAttributeStruct<Vector3Int> {

    /*
     * GLSL ivec3 uniform. Holds its own vector and copies incoming values
     * into it, so setting it never allocates.
     */

    // Constructor \\

    public Vector3IntUniform() {
        super(UniformType.VECTOR3_INT, new Vector3Int());
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector3IntUniform();
    }

    // Push \\

    @Override
    protected void push(int handle, Vector3Int value) {
        EngineContext.gl20.glUniform3i(handle, value.x, value.y, value.z);
    }

    // Accessible \\

    @Override
    protected void applyValue(Vector3Int value) {
        this.value.set(value);
    }

    @Override
    protected void applyObject(Object value) {

        if (value instanceof Vector3Int vector)
            applyValue(vector);
        else
            throwException("Vector3IntUniform expects Vector3Int, got " + value.getClass().getSimpleName());
    }
}
