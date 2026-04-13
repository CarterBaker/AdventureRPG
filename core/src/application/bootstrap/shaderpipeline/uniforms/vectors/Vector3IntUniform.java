package application.bootstrap.shaderpipeline.uniforms.vectors;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import application.core.engine.EngineContext;
import application.core.util.mathematics.vectors.Vector3Int;

public final class Vector3IntUniform extends UniformAttributeStruct<Vector3Int> {

    public Vector3IntUniform() {
        super(UniformType.VECTOR3_INT, new Vector3Int());
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector3IntUniform();
    }

    @Override
    protected void push(int handle, Vector3Int value) {
        EngineContext.gl.glUniform3i(handle, value.x, value.y, value.z);
    }

    @Override
    protected void applyValue(Vector3Int value) {
        this.value.set(value);
    }
}
