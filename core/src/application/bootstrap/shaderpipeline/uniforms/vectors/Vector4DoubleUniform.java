package application.bootstrap.shaderpipeline.uniforms.vectors;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;
import engine.util.mathematics.vectors.Vector4Double;

public final class Vector4DoubleUniform extends UniformAttributeStruct<Vector4Double> {

    public Vector4DoubleUniform() {
        super(UniformType.VECTOR4_DOUBLE, new Vector4Double());
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector4DoubleUniform();
    }

    @Override
    protected void push(int handle, Vector4Double value) {
        EngineContext.gl.glUniform4f(handle, (float) value.x, (float) value.y, (float) value.z, (float) value.w);
    }

    @Override
    protected void applyValue(Vector4Double value) {
        this.value.set(value);
    }
}
