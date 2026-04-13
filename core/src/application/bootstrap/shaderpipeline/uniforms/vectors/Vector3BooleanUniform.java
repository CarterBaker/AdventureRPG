package application.bootstrap.shaderpipeline.uniforms.vectors;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;
import engine.util.mathematics.vectors.Vector3Boolean;

public final class Vector3BooleanUniform extends UniformAttributeStruct<Vector3Boolean> {

    public Vector3BooleanUniform() {
        super(UniformType.VECTOR3_BOOLEAN, new Vector3Boolean());
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector3BooleanUniform();
    }

    @Override
    protected void push(int handle, Vector3Boolean value) {
        EngineContext.gl.glUniform3i(handle, value.x ? 1 : 0, value.y ? 1 : 0, value.z ? 1 : 0);
    }

    @Override
    protected void applyValue(Vector3Boolean value) {
        this.value.set(value);
    }
}
