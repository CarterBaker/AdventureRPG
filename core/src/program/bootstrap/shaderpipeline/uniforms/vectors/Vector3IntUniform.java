package program.bootstrap.shaderpipeline.uniforms.vectors;

import program.core.engine.EngineContext;
import program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import program.bootstrap.shaderpipeline.uniforms.UniformType;
import program.core.util.mathematics.vectors.Vector3Int;

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
