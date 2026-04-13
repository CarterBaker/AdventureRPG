package application.bootstrap.shaderpipeline.uniforms.vectors;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import application.core.engine.EngineContext;
import application.core.util.mathematics.vectors.Vector2Boolean;

public final class Vector2BooleanUniform extends UniformAttributeStruct<Vector2Boolean> {

    public Vector2BooleanUniform() {
        super(UniformType.VECTOR2_BOOLEAN, new Vector2Boolean());
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new Vector2BooleanUniform();
    }

    @Override
    protected void push(int handle, Vector2Boolean value) {
        EngineContext.gl.glUniform2i(handle, value.x ? 1 : 0, value.y ? 1 : 0);
    }

    @Override
    protected void applyValue(Vector2Boolean value) {
        this.value.set(value);
    }
}
