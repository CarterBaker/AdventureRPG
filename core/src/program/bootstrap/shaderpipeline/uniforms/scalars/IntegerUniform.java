package program.bootstrap.shaderpipeline.uniforms.scalars;

import program.core.engine.EngineContext;
import program.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import program.bootstrap.shaderpipeline.uniforms.UniformType;

public final class IntegerUniform extends UniformAttributeStruct<Integer> {

    public IntegerUniform() {
        super(UniformType.INT, 0);
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new IntegerUniform();
    }

    @Override
    protected void push(int handle, Integer value) {
        EngineContext.gl.glUniform1i(handle, value);
    }

    @Override
    protected void applyValue(Integer value) {
        this.value = value;
    }
}
