package application.bootstrap.shaderpipeline.uniforms.scalars;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;

public final class BooleanUniform extends UniformAttributeStruct<Boolean> {

    /*
     * GLSL bool uniform, uploaded as an int.
     */

    // Constructor \\

    public BooleanUniform() {
        super(UniformType.BOOL, false);
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new BooleanUniform();
    }

    // Push \\

    @Override
    protected void push(int handle, Boolean value) {
        EngineContext.gl20.glUniform1i(handle, value ? 1 : 0);
    }

    // Accessible \\

    @Override
    protected void applyValue(Boolean value) {
        this.value = value;
    }
}
