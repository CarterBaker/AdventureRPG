package application.bootstrap.shaderpipeline.uniforms.scalars;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;

public final class BooleanUniform extends UniformAttributeStruct<Boolean> {

    public BooleanUniform() {
        super(UniformType.BOOL, false);
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new BooleanUniform();
    }

    @Override
    protected void push(int handle, Boolean value) {
        EngineContext.gl.glUniform1i(handle, value ? 1 : 0);
    }

    @Override
    protected void applyValue(Boolean value) {
        this.value = value;
    }
}
