package application.bootstrap.shaderpipeline.uniforms.vectors;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;
import engine.util.mathematics.vectors.Vector3Double;

public final class Vector3DoubleUniform extends UniformAttributeStruct<Vector3Double> {

    public Vector3DoubleUniform() {
        super(UniformType.VECTOR3_DOUBLE, new Vector3Double());
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector3DoubleUniform();
    }

    @Override
    protected void push(int handle, Vector3Double value) {
        EngineContext.gl20.glUniform3f(handle, (float) value.x, (float) value.y, (float) value.z);
    }

    @Override
    protected void applyValue(Vector3Double value) {
        this.value.set(value);
    }
}
