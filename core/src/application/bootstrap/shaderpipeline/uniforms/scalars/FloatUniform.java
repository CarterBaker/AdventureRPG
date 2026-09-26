package application.bootstrap.shaderpipeline.uniforms.scalars;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;

public final class FloatUniform extends UniformAttributeStruct<Float> {

    /*
     * GLSL float uniform.
     */

    // Constructor \\

    public FloatUniform() {
        super(UniformType.FLOAT, 0f);
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new FloatUniform();
    }

    // Push \\

    @Override
    protected void push(int handle, Float value) {
        EngineContext.gl20.glUniform1f(handle, value);
    }

    // Accessible \\

    @Override
    protected void applyValue(Float value) {
        this.value = value;
    }
}
