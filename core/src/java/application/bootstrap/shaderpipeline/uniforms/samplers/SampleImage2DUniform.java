package application.bootstrap.shaderpipeline.uniforms.samplers;

import application.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import application.bootstrap.shaderpipeline.uniforms.UniformType;
import engine.root.EngineContext;
import engine.root.EngineSetting;

public final class SampleImage2DUniform extends UniformAttributeStruct<Integer> {

    /*
     * GLSL sampler2D uniform. Holds a texture handle, binds it to the unit the
     * render pass assigns, and uploads that unit.
     */

    // Internal
    private int textureUnit;

    // Constructor \\

    public SampleImage2DUniform() {
        super(UniformType.SAMPLE_IMAGE_2D, 0);
    }

    @Override
    public UniformAttributeStruct<?> createDefault() {
        return new SampleImage2DUniform();
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
        EngineContext.gl20.glBindTexture(EngineSetting.GL_TEXTURE_2D, value);
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
