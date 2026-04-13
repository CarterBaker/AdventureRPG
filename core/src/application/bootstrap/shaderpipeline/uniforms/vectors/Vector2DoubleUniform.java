package application.bootstrap.shaderpipeline.uniforms.vectors;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import application.core.engine.EngineContext;
import application.core.util.mathematics.vectors.Vector2Double;

public final class Vector2DoubleUniform extends UniformAttributeStruct<Vector2Double> {

    public Vector2DoubleUniform() {
        super(UniformType.VECTOR2_DOUBLE, new Vector2Double());
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector2DoubleUniform();
    }

    @Override
    protected void push(int handle, Vector2Double value) {
        EngineContext.gl.glUniform2f(handle, (float) value.x, (float) value.y);
    }

    @Override
    protected void applyValue(Vector2Double value) {
        this.value.set(value);
    }
}
