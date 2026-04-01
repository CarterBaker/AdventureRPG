package program.bootstrap.shaderpipeline.uniforms.scalars;

import program.core.app.CoreContext;
import program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import program.bootstrap.shaderpipeline.uniforms.UniformType;

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
        CoreContext.gl.glUniform1i(handle, value ? 1 : 0);
    }

    @Override
    protected void applyValue(Boolean value) {
        this.value = value;
    }
}
