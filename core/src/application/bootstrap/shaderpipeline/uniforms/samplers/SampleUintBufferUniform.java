package application.bootstrap.shaderpipeline.uniforms.samplers;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;
import engine.root.EngineSetting;

public final class SampleUintBufferUniform extends UniformAttributeStruct<Integer> {

    /*
     * GLSL usamplerBuffer uniform. Holds a buffer texture handle — a view of
     * packed integer records a shader pulls by index — binds it to the unit
     * the render pass assigns, and uploads that unit.
     */

    // Internal
    private int textureUnit;

    // Constructor \\

    public SampleUintBufferUniform() {
        super(UniformType.SAMPLE_UINT_BUFFER, 0);
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new SampleUintBufferUniform();
    }

    // Sampler \\

    @Override
    public boolean isSampler() {
        return true;
    }

    @Override
    public void bindTexture(int unit) {
        this.textureUnit = unit;
        EngineContext.gl20.glActiveTexture(EngineSetting.GL_TEXTURE0 + unit);
        EngineContext.gl30.glBindTexture(EngineSetting.GL_TEXTURE_BUFFER, value);
    }

    // Push \\

    @Override
    protected void push(int handle, Integer value) {
        EngineContext.gl20.glUniform1i(handle, textureUnit);
    }

    // Accessible \\

    @Override
    protected void applyValue(Integer value) {
        this.value = value;
    }
}
