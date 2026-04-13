package application.bootstrap.shaderpipeline.uniforms.scalars;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;

public final class DoubleUniform extends UniformAttributeStruct<Double> {

    public DoubleUniform() {
        super(UniformType.DOUBLE, 0.0);
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new DoubleUniform();
    }

    @Override
    protected void push(int handle, Double value) {
        EngineContext.gl.glUniform1f(handle, value.floatValue());
    }

    @Override
    protected void applyValue(Double value) {
        this.value = value;
    }
}
